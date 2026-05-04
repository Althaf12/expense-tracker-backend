package com.expensetracker.service;

import com.expensetracker.exception.BankStatementProcessingException;
import com.expensetracker.model.UserPreferences;
import com.expensetracker.repository.UserPreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Manages storing, retrieving and verifying the HDFC bank statement PDF password
 * in the {@code user_preferences} table using AES-256 encryption.
 *
 * <h3>Encryption scheme</h3>
 * AES/CBC/PKCS5Padding with a random 16-byte IV prepended to the ciphertext.
 * The stored value is {@code Base64(IV || ciphertext)}.
 */
@Service
public class BankStatementPasswordService {

    private static final Logger logger = LoggerFactory.getLogger(BankStatementPasswordService.class);

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    private final byte[] encryptionKey;
    private final UserPreferencesRepository preferencesRepository;

    public BankStatementPasswordService(
            @Value("${app.bank-statement.encryption-key}") String rawKey,
            UserPreferencesRepository preferencesRepository) {
        // Key must be exactly 32 bytes for AES-256; pad / trim as needed
        byte[] keyBytes = rawKey.getBytes(StandardCharsets.UTF_8);
        byte[] fixed = new byte[32];
        System.arraycopy(keyBytes, 0, fixed, 0, Math.min(keyBytes.length, 32));
        this.encryptionKey       = fixed;
        this.preferencesRepository = preferencesRepository;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /** Returns {@code true} if the user already has a stored bank-statement password. */
    public boolean hasStoredPassword(String userId) {
        return preferencesRepository.findByUserId(userId)
                .map(p -> p.getBankStatementPassword() != null && !p.getBankStatementPassword().isBlank())
                .orElse(false);
    }

    /**
     * Retrieves and decrypts the stored password for the given user.
     *
     * @return the plain-text password, or {@code null} if none is stored
     */
    public String getDecryptedPassword(String userId) {
        Optional<UserPreferences> prefs = preferencesRepository.findByUserId(userId);
        if (prefs.isEmpty()) return null;
        String stored = prefs.get().getBankStatementPassword();
        if (stored == null || stored.isBlank()) return null;
        try {
            return decrypt(stored);
        } catch (Exception e) {
            logger.error("Failed to decrypt stored bank statement password for userId={}", userId, e);
            throw new BankStatementProcessingException(
                    "Failed to decrypt the stored PDF password. Please provide the password manually.");
        }
    }

    /**
     * Encrypts {@code plainPassword} and persists it in the user's preferences row.
     * If no preferences row exists yet, one is created.
     *
     * @param userId        the user
     * @param plainPassword the plain-text PDF password to store
     */
    @Transactional
    public void storePassword(String userId, String plainPassword) {
        try {
            String encrypted = encrypt(plainPassword);
            UserPreferences prefs = preferencesRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        UserPreferences p = new UserPreferences();
                        p.setUserId(userId);
                        return p;
                    });
            prefs.setBankStatementPassword(encrypted);
            preferencesRepository.save(prefs);
            logger.info("Bank statement password stored for userId={}", userId);
        } catch (BankStatementProcessingException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to encrypt/store bank statement password for userId={}", userId, e);
            throw new BankStatementProcessingException(
                    "Failed to store the PDF password securely. Please try again.");
        }
    }

    /**
     * Removes any stored bank-statement password for the user.
     */
    @Transactional
    public void clearPassword(String userId) {
        preferencesRepository.findByUserId(userId).ifPresent(prefs -> {
            prefs.setBankStatementPassword(null);
            preferencesRepository.save(prefs);
            logger.info("Bank statement password cleared for userId={}", userId);
        });
    }

    // ── Crypto helpers ───────────────────────────────────────────────────────

    private String encrypt(String plainText) throws Exception {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(encryptionKey, "AES"),
                new IvParameterSpec(iv));

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Store IV + cipherText together so we can decrypt later
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    private String decrypt(String base64Combined) throws Exception {
        byte[] combined = Base64.getDecoder().decode(base64Combined);

        byte[] iv         = new byte[16];
        byte[] cipherText = new byte[combined.length - 16];
        System.arraycopy(combined, 0, iv, 0, 16);
        System.arraycopy(combined, 16, cipherText, 0, cipherText.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(encryptionKey, "AES"),
                new IvParameterSpec(iv));

        return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    }
}

