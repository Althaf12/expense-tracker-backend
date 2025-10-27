package com.expensetracker.service;

import com.expensetracker.model.User;
import com.expensetracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncryptionService encryptionService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    // Password rules: 8-16 chars, at least 1 uppercase, 1 digit, 1 special (allowed set), disallow quotes, commas, hyphens etc.
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.{8,16}$)(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%&*()_+=<>?\\[\\]{}\\.:;\\/\\\\|~`^]).+$");

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    public User createOrUpdateUser(User user) {
        // If userId empty -> new user: generate GUID
        if (user.getUserId() == null || user.getUserId().isBlank()) {
            user.setUserId(UUID.randomUUID().toString());
        }
        // Validate email if present
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            if (!EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
                throw new IllegalArgumentException("invalid email format");
            }
        }
        // Validate password if present and hash
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            if (!PASSWORD_PATTERN.matcher(user.getPassword()).matches()) {
                throw new IllegalArgumentException("password rules violated: 8-16 chars, 1 uppercase, 1 digit, 1 special char; disallowed characters like quotes, comma, hyphen are not allowed");
            }
            String hashed = encryptionService.hash(user.getPassword());
            user.setPassword(hashed);
        }

        if (user.getCreatedTmstp() == null) {
            user.setCreatedTmstp(LocalDateTime.now());
        }
        user.setLastUpdateTmstp(LocalDateTime.now());
        return userRepository.save(user);
    }

    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    private User performPasswordUpdate(User u, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) throw new IllegalArgumentException("new password required");
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new IllegalArgumentException("password rules violated: 8-16 chars, 1 uppercase, 1 digit, 1 special char; disallowed characters like quotes, comma, hyphen are not allowed");
        }
        // Check if new password equals existing by comparing hashed values
        boolean same = encryptionService.matches(newPassword, u.getPassword());
        if (same) {
            throw new IllegalArgumentException("new password must be different from current password");
        }
        String hashed = encryptionService.hash(newPassword);
        u.setPassword(hashed);
        u.setLastUpdateTmstp(LocalDateTime.now());
        return userRepository.save(u);
    }

    public User updatePasswordByUserId(String userId, String newPassword) {
        Optional<User> opt = userRepository.findById(userId);
        if (opt.isEmpty()) throw new IllegalArgumentException("user not found");
        return performPasswordUpdate(opt.get(), newPassword);
    }

    public User updatePasswordByUsername(String username, String newPassword) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) throw new IllegalArgumentException("user not found");
        return performPasswordUpdate(opt.get(), newPassword);
    }

    public User updatePasswordByEmail(String email, String newPassword) {
        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) throw new IllegalArgumentException("user not found");
        return performPasswordUpdate(opt.get(), newPassword);
    }
}
