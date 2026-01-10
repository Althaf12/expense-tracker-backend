package com.expensetracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * Provides JWT token validation and claims extraction.
 * This service validates tokens issued by the central Auth service.
 *
 * IMPORTANT: The key derivation logic MUST match the Auth service exactly.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;

    public JwtTokenProvider(@Value("${jwt.secret}") String jwtSecret) {
        // Derive the signing key using the SAME logic as Auth service
        byte[] keyBytes = deriveKeyBytes(jwtSecret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);

        // Log partial secret for debugging (first 4 and last 4 chars only)
        String masked = jwtSecret.length() > 8
            ? jwtSecret.substring(0, 4) + "..." + jwtSecret.substring(jwtSecret.length() - 4)
            : "***";
        log.info("JwtTokenProvider initialized - secret length: {} chars, preview: {}, derived key length: {} bytes",
                jwtSecret.length(), masked, keyBytes.length);
    }

    /**
     * Derives key bytes using the SAME logic as Auth service's JwtTokenProvider.
     * This ensures both services compute identical signing keys from the same jwt.secret.
     *
     * Logic:
     * 1. Try Base64 decode first
     * 2. If decoded bytes >= 32, use them directly
     * 3. Otherwise, use raw UTF-8 bytes
     * 4. If raw bytes < 32, derive a 32-byte key via SHA-256
     */
    private byte[] deriveKeyBytes(String jwtSecret) {
        byte[] keyBytes;

        // Try Base64 decode first
        byte[] decoded = null;
        try {
            decoded = Base64.getDecoder().decode(jwtSecret);
        } catch (IllegalArgumentException ex) {
            // Not valid Base64, that's okay
            decoded = null;
        }

        if (decoded != null && decoded.length >= 32) {
            // Valid Base64 with sufficient length
            keyBytes = decoded;
            log.debug("Using Base64-decoded JWT secret ({} bytes)", keyBytes.length);
        } else {
            // Use raw UTF-8 bytes
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);

            if (keyBytes.length < 32) {
                // Derive a 32-byte key via SHA-256 (same as Auth service)
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    keyBytes = digest.digest(keyBytes);
                    log.debug("JWT secret was short ({} bytes), derived 256-bit key via SHA-256",
                            jwtSecret.getBytes(StandardCharsets.UTF_8).length);
                } catch (NoSuchAlgorithmException e) {
                    // Fallback: pad with zeros (matches Auth service fallback)
                    byte[] padded = new byte[32];
                    System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
                    keyBytes = padded;
                    log.warn("SHA-256 not available, using zero-padded key");
                }
            } else {
                log.debug("Using raw UTF-8 JWT secret ({} bytes)", keyBytes.length);
            }
        }

        return keyBytes;
    }

    /**
     * Validates the JWT token.
     * Matches Auth service validation: verifies signature and checks token type is "access".
     *
     * @param token the JWT token to validate
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Check if token is an access token (matches Auth service validation)
            String tokenType = claims.get("type", String.class);
            if (tokenType != null && !"access".equals(tokenType)) {
                log.warn("JWT token is not an access token, type: {}", tokenType);
                return false;
            }

            log.debug("JWT token validated successfully. Subject: {}, Issuer: {}, Expiration: {}, Type: {}",
                    claims.getSubject(), claims.getIssuer(), claims.getExpiration(), tokenType);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature - token was signed with a different key: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token - malformed: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token - expired at: {}", ex.getClaims().getExpiration());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Extracts the user ID (subject) from the JWT token.
     *
     * @param token the JWT token
     * @return the user ID as UUID
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extracts the username from the JWT token.
     *
     * @param token the JWT token
     * @return the username, or null if not present
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("username", String.class);
    }

    /**
     * Extracts the email from the JWT token.
     * Useful for OAuth users where email is the primary identifier.
     *
     * @param token the JWT token
     * @return the email, or null if not present
     */
    public String getEmailFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("email", String.class);
    }

    /**
     * Extracts the user_id claim from the JWT token.
     * This is the string user ID used in the expense tracker tables.
     *
     * @param token the JWT token
     * @return the user_id string
     */
    public String getUserIdStringFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        // Try to get user_id claim first, fall back to subject
        String userId = claims.get("user_id", String.class);
        if (userId == null) {
            userId = claims.getSubject();
        }
        return userId;
    }

    /**
     * Extracts the issuer from the JWT token.
     *
     * @param token the JWT token
     * @return the issuer
     */
    public String getIssuerFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getIssuer();
    }

    /**
     * Extracts all claims from the JWT token.
     *
     * @param token the JWT token
     * @return the Claims object
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

