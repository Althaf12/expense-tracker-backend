package com.expensetracker.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter that validates tokens from cookies or Authorization header.
 * Integrates with the central Auth service's JWT tokens.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        log.debug("Processing request: {} {}", method, requestURI);

        try {
            // Guard: if SecurityContext already has an authenticated principal, skip processing
            if (SecurityContextHolder.getContext().getAuthentication() != null &&
                    SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                    !"anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal())) {
                log.debug("Already authenticated, skipping JWT filter");
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = getJwtFromRequest(request);

            if (jwt == null) {
                log.debug("No JWT token found in request for {} {}", method, requestURI);
            } else {
                log.debug("JWT token found, validating...");
            }

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String userId = tokenProvider.getUserIdStringFromToken(jwt);
                String username = tokenProvider.getUsernameFromToken(jwt);

                log.info("Authenticated user: {} (userId: {}) for {} {}", username, userId, method, requestURI);

                // Extract roles/authorities from token claims, if present
                Collection<GrantedAuthority> authorities = new ArrayList<>();
                try {
                    Claims claims = tokenProvider.getAllClaimsFromToken(jwt);
                    Object rolesObj = claims.get("roles");

                    if (rolesObj instanceof String) {
                        String rolesStr = (String) rolesObj;
                        authorities = Arrays.stream(rolesStr.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());
                    } else if (rolesObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (List<Object>) rolesObj;
                        authorities = list.stream()
                                .filter(Objects::nonNull)
                                .map(Object::toString)
                                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());
                    }
                } catch (Exception ex) {
                    log.debug("Failed to extract roles from JWT claims, will fall back to default role", ex);
                }

                if (authorities.isEmpty()) {
                    authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                }

                // Use userId as the principal - this matches the user_id in database tables
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context for {} {}: {}",
                    method, requestURI, ex.getMessage(), ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // First, try to get JWT from HttpOnly cookie (primary method for SSO)
        String jwtFromCookie = getJwtFromCookie(request);
        if (StringUtils.hasText(jwtFromCookie)) {
            return jwtFromCookie;
        }

        // Fallback to Authorization header for backward compatibility and API clients
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String getJwtFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            log.debug("No cookies present in request");
            return null;
        }

        log.debug("Found {} cookies in request", cookies.length);
        for (Cookie cookie : cookies) {
            log.debug("Cookie: {} = {}", cookie.getName(),
                    cookie.getName().equals(CookieService.ACCESS_TOKEN_COOKIE) ? "[JWT TOKEN]" : cookie.getValue());
            if (CookieService.ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                log.debug("Found access_token cookie");
                return cookie.getValue();
            }
        }
        log.debug("access_token cookie not found among {} cookies", cookies.length);
        return null;
    }
}

