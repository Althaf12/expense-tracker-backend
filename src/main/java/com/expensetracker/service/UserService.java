package com.expensetracker.service;

import com.expensetracker.model.User;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@CacheConfig(cacheNames = "users")
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncryptionService encryptionService;
    private final MailService mailService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    // Password rules: 8-16 chars, at least 1 uppercase, 1 digit, 1 special (allowed set), disallow quotes, commas, hyphens etc.
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.{8,16}$)(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%&*()_+=<>?\\[\\]{}\\.:;\\/\\\\|~`^]).+$");

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncryptionService encryptionService, MailService mailService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.mailService = mailService;
    }

    @CacheEvict(allEntries = true)
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

    @Cacheable(key = "#userId")
    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    @CacheEvict(allEntries = true)
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    @Cacheable(key = "#username")
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Cacheable(key = "#email")
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @CacheEvict(allEntries = true)
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

    @CacheEvict(allEntries = true)
    public User updatePasswordByUserId(String userId, String newPassword) {
        Optional<User> opt = userRepository.findById(userId);
        if (opt.isEmpty()) throw new IllegalArgumentException("user not found");
        return performPasswordUpdate(opt.get(), newPassword);
    }

    @CacheEvict(allEntries = true)
    public User updatePasswordByUsername(String username, String newPassword) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) throw new IllegalArgumentException("user not found");
        return performPasswordUpdate(opt.get(), newPassword);
    }

    @CacheEvict(allEntries = true)
    public User updatePasswordByEmail(String email, String newPassword) {
        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) throw new IllegalArgumentException("user not found");
        return performPasswordUpdate(opt.get(), newPassword);
    }

    // --- Password reset flow ---
    // Generate a reset token, save it with expiry (e.g., 1 hour), and send email via MailService (which may be a stub).
    @CacheEvict(allEntries = true)
    public String generatePasswordResetToken(String emailOrUsername) {
        Optional<User> opt = userRepository.findByEmail(emailOrUsername);
        if (opt.isEmpty()) {
            opt = userRepository.findByUsername(emailOrUsername);
            if (opt.isEmpty()) throw new IllegalArgumentException("user not found");
        }
        User u = opt.get();
        String token = UUID.randomUUID().toString();
        u.setResetToken(token);
        u.setResetTokenExpiry(LocalDateTime.now().plus(1, ChronoUnit.HOURS));
        userRepository.save(u);
        // send email with link (stubbed if MailService not configured)
        String resetLink = String.format(Constants.RESET_URL, token);
        mailService.sendPasswordResetEmail(u.getEmail(), resetLink);
        return token;
    }

    @CacheEvict(allEntries = true)
    public User resetPasswordWithToken(String token, String newPassword) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("token required");
        Optional<User> opt = userRepository.findByResetToken(token);
        if (opt.isEmpty()) throw new IllegalArgumentException("invalid token");
        User u = opt.get();
        if (u.getResetTokenExpiry() == null || u.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("token expired");
        }
        // Validate and set new password
        if (newPassword == null || newPassword.isBlank()) throw new IllegalArgumentException("new password required");
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new IllegalArgumentException("password rules violated: 8-16 chars, 1 uppercase, 1 digit, 1 special char; disallowed characters like quotes, comma, hyphen are not allowed");
        }
        // Ensure not same as current
        if (encryptionService.matches(newPassword, u.getPassword())) {
            throw new IllegalArgumentException("new password must be different from current password");
        }
        String hashed = encryptionService.hash(newPassword);
        u.setPassword(hashed);
        // clear token
        u.setResetToken(null);
        u.setResetTokenExpiry(null);
        u.setLastUpdateTmstp(LocalDateTime.now());
        return userRepository.save(u);
    }
}
