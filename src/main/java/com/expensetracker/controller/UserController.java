package com.expensetracker.controller;

import com.expensetracker.dto.UserRequest;
import com.expensetracker.dto.UserResponse;
import com.expensetracker.model.User;
import com.expensetracker.service.UserService;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.IncomeService;
import com.expensetracker.service.UserExpenseCategoryService;
import com.expensetracker.service.UserPreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final UserExpenseCategoryService userExpenseCategoryService;
    private final UserPreferencesService userPreferencesService;

    @Autowired
    public UserController(UserService userService,
                          ExpenseService expenseService,
                          IncomeService incomeService,
                          UserExpenseCategoryService userExpenseCategoryService,
                          UserPreferencesService userPreferencesService) {
        this.userService = userService;
        this.expenseService = expenseService;
        this.incomeService = incomeService;
        this.userExpenseCategoryService = userExpenseCategoryService;
        this.userPreferencesService = userPreferencesService;
    }

    @PostMapping("")
    public ResponseEntity<?> createOrUpdateUser(@RequestBody UserRequest request) {
        if (request == null) {
            logger.warn("createOrUpdateUser called with null request");
            return ResponseEntity.badRequest().body(Map.of("error", "request required"));
        }
        logger.info("createOrUpdateUser called for username={}", request.getUsername());
        User u = new User();
        u.setUsername(request.getUsername());
        u.setEmail(request.getEmail());
        u.setPassword(request.getPassword());
        try {
            boolean isNewUser = (u.getUserId() == null || u.getUserId().isBlank());
            User saved = userService.createOrUpdateUser(u);
            // If this is a new user, copy master categories to user expense categories
            if (isNewUser && saved.getUsername() != null && !saved.getUsername().isBlank()) {
                userExpenseCategoryService.onUserCreated(saved.getUsername());
                // Create default user preferences for the new user (idempotent)
                try {
                    userPreferencesService.createDefaultsForUser(saved.getUsername());
                } catch (Exception ex) {
                    // Log exception so we can troubleshoot preference creation failures without failing user creation
                    logger.warn("Failed to create default preferences for user {}", saved.getUsername(), ex);
                }
            }
            logger.info("User created/updated: username={}", saved.getUsername());
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (IllegalArgumentException ex) {
            logger.warn("Validation error during createOrUpdateUser: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        if (userId == null || userId.isBlank()) {
            logger.warn("deleteUser called with empty userId");
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        var optUser = userService.findById(userId);
        if (optUser.isEmpty()) {
            logger.warn("deleteUser called for non-existing userId={}", userId);
            return ResponseEntity.badRequest().body(Map.of("error", "user does not exist"));
        }
        User user = optUser.get();
        String username = user.getUsername();
        // delete expenses, incomes, and user expense categories in batch using repository-level deletes
        try {
            expenseService.deleteAllByUsername(username);
            incomeService.deleteAllByUsername(username);
            userExpenseCategoryService.deleteAll(username);
            // finally delete the user record
            userService.deleteUser(userId);
            logger.info("Deleted user and related data for username={}", username);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception ex) {
            logger.error("Failed to delete user {} and related data", username, ex);
            return ResponseEntity.status(500).body(Map.of("error", "failed to delete user and related data"));
        }
    }

    @PostMapping("/details")
    public ResponseEntity<?> getUserDetails(@RequestBody Map<String, String> body) {
        if (body == null) {
            logger.warn("getUserDetails called with null body");
            return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        }
        String username = body.get("username");
        String email = body.get("email");
        if ((username == null || username.isBlank()) && (email == null || email.isBlank())) {
            logger.warn("getUserDetails called without username or email");
            return ResponseEntity.badRequest().body(Map.of("error", "provide username or email"));
        }
        try {
            User user;
            if (username != null && !username.isBlank()) {
                var opt = userService.findByUsername(username);
                if (opt.isEmpty()) {
                    logger.warn("getUserDetails: user not found username={}", username);
                    return ResponseEntity.status(404).body(Map.of("error", "user not found"));
                }
                user = opt.get();
            } else {
                var opt = userService.findByEmail(email);
                if (opt.isEmpty()) {
                    logger.warn("getUserDetails: user not found email={}", email);
                    return ResponseEntity.status(404).body(Map.of("error", "user not found"));
                }
                user = opt.get();
            }
            UserResponse resp = new UserResponse();
            resp.setUserId(user.getUserId());
            resp.setUsername(user.getUsername());
            resp.setEmail(user.getEmail());
            resp.setCreatedTmstp(user.getCreatedTmstp());
            resp.setLastUpdateTmstp(user.getLastUpdateTmstp());
            logger.info("getUserDetails successful for username={}", user.getUsername());
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            logger.error("Internal error in getUserDetails", ex);
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        if (body == null) {
            logger.warn("forgotPassword called with null body");
            return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        }
        String usernameOrEmail = body.get("username") != null ? body.get("username") : body.get("email");
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            logger.warn("forgotPassword called without username or email");
            return ResponseEntity.badRequest().body(Map.of("error", "provide username or email"));
        }
        try {
            String token = userService.generatePasswordResetToken(usernameOrEmail);
            logger.info("Password reset token generated for {}", usernameOrEmail);
            return ResponseEntity.ok(Map.of("status", "reset email sent", "token", token));
        } catch (IllegalArgumentException ex) {
            logger.warn("forgotPassword: {}", ex.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("Internal error in forgotPassword", ex);
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        if (body == null) {
            logger.warn("resetPassword called with null body");
            return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        }
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        if (token == null || token.isBlank()) {
            logger.warn("resetPassword called without token");
            return ResponseEntity.badRequest().body(Map.of("error", "token required"));
        }
        if (newPassword == null || newPassword.isBlank()) {
            logger.warn("resetPassword called without newPassword");
            return ResponseEntity.badRequest().body(Map.of("error", "newPassword required"));
        }
        try {
            User u = userService.resetPasswordWithToken(token, newPassword);
            u.setPassword(null);
            logger.info("Password reset successful for user {}", u.getUsername());
            return ResponseEntity.ok(Map.of("status", "password reset"));
        } catch (IllegalArgumentException ex) {
            logger.warn("resetPassword validation error: {}", ex.getMessage());
            return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("Internal error in resetPassword", ex);
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> updateUserPassword(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.get("email");
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            logger.warn("updateUserPassword called without newPassword");
            return ResponseEntity.badRequest().body(Map.of("error", "newPassword required"));
        }
        try {
            if (username != null && !username.isBlank()) {
                userService.updatePasswordByUsername(username, newPassword);
                logger.info("Password updated for username={}", username);
                return ResponseEntity.ok(Map.of("status", "success"));
            } else if (email != null && !email.isBlank()) {
                userService.updatePasswordByEmail(email, newPassword);
                logger.info("Password updated for email={}", email);
                return ResponseEntity.ok(Map.of("status", "success"));
            }
            logger.warn("updateUserPassword called without identifier");
            return ResponseEntity.badRequest().body(Map.of("error", "provide userId or username or email"));
        } catch (IllegalArgumentException ex) {
            logger.warn("updateUserPassword error: {}", ex.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }
}
