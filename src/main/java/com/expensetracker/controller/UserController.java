package com.expensetracker.controller;

import com.expensetracker.dto.UserRequest;
import com.expensetracker.dto.UserResponse;
import com.expensetracker.model.User;
import com.expensetracker.service.UserService;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.IncomeService;
import com.expensetracker.service.UserExpenseCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final UserExpenseCategoryService userExpenseCategoryService;

    @Autowired
    public UserController(UserService userService, ExpenseService expenseService, IncomeService incomeService, UserExpenseCategoryService userExpenseCategoryService) {
        this.userService = userService;
        this.expenseService = expenseService;
        this.incomeService = incomeService;
        this.userExpenseCategoryService = userExpenseCategoryService;
    }

    @PostMapping("")
    public ResponseEntity<?> createOrUpdateUser(@RequestBody UserRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "request required"));
        }
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
            }
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        var optUser = userService.findById(userId);
        if (optUser.isEmpty()) {
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
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "failed to delete user and related data"));
        }
    }

    @PostMapping("/details")
    public ResponseEntity<?> getUserDetails(@RequestBody Map<String, String> body) {
        if (body == null) return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        String username = body.get("username");
        String email = body.get("email");
        if ((username == null || username.isBlank()) && (email == null || email.isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "provide username or email"));
        }
        try {
            User user;
            if (username != null && !username.isBlank()) {
                var opt = userService.findByUsername(username);
                if (opt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "user not found"));
                user = opt.get();
            } else {
                var opt = userService.findByEmail(email);
                if (opt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "user not found"));
                user = opt.get();
            }
            UserResponse resp = new UserResponse();
            resp.setUserId(user.getUserId());
            resp.setUsername(user.getUsername());
            resp.setEmail(user.getEmail());
            resp.setCreatedTmstp(user.getCreatedTmstp());
            resp.setLastUpdateTmstp(user.getLastUpdateTmstp());
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        if (body == null) return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        String usernameOrEmail = body.get("username") != null ? body.get("username") : body.get("email");
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "provide username or email"));
        try {
            String token = userService.generatePasswordResetToken(usernameOrEmail);
            return ResponseEntity.ok(Map.of("status", "reset email sent", "token", token));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        if (body == null) return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        if (token == null || token.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "token required"));
        if (newPassword == null || newPassword.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "newPassword required"));
        try {
            User u = userService.resetPasswordWithToken(token, newPassword);
            u.setPassword(null);
            return ResponseEntity.ok(Map.of("status", "password reset"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> updateUserPassword(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.get("email");
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "newPassword required"));
        try {
            if (username != null && !username.isBlank()) {
                userService.updatePasswordByUsername(username, newPassword);
                return ResponseEntity.ok(Map.of("status", "success"));
            } else if (email != null && !email.isBlank()) {
                userService.updatePasswordByEmail(email, newPassword);
                return ResponseEntity.ok(Map.of("status", "success"));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "provide userId or username or email"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }
}
