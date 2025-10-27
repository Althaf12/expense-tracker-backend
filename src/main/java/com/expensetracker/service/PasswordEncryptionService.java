package com.expensetracker.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordEncryptionService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    // Hash the plain password with BCrypt
    public String hash(String plainPassword) {
        return encoder.encode(plainPassword);
    }

    // Check whether plainPassword matches the stored hash
    public boolean matches(String plainPassword, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) return false;
        try {
            return encoder.matches(plainPassword, storedHash);
        } catch (Exception ex) {
            return false;
        }
    }
}
