package com.expensetracker.controller;

import com.expensetracker.dto.UserRequest;
import com.expensetracker.dto.UserResponse;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.UserNotFoundException;
import com.expensetracker.model.User;
import com.expensetracker.service.UserService;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.IncomeService;
import com.expensetracker.service.UserExpenseCategoryService;
import com.expensetracker.service.UserPreferencesService;
import com.expensetracker.admin.service.PlannedExpensesService;
import com.expensetracker.service.MonthlyBalanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
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
    private final PlannedExpensesService plannedExpensesService;
    private final MonthlyBalanceService monthlyBalanceService;

    @Autowired
    public UserController(UserService userService,
                          ExpenseService expenseService,
                          IncomeService incomeService,
                          UserExpenseCategoryService userExpenseCategoryService,
                          UserPreferencesService userPreferencesService,
                          @Qualifier("adminPlannedExpensesService") PlannedExpensesService plannedExpensesService,
                          MonthlyBalanceService monthlyBalanceService) {
        this.userService = userService;
        this.expenseService = expenseService;
        this.incomeService = incomeService;
        this.userExpenseCategoryService = userExpenseCategoryService;
        this.userPreferencesService = userPreferencesService;
        this.plannedExpensesService = plannedExpensesService;
        this.monthlyBalanceService = monthlyBalanceService;
    }

    @PostMapping("")
    public ResponseEntity<?> createOrUpdateUser(@RequestBody UserRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId().isBlank()) {
            logger.warn("createOrUpdateUser called with null or empty userId");
            throw new BadRequestException("userId is required");
        }
        String userId = request.getUserId().trim();
        logger.info("createOrUpdateUser called for userId={}", userId);

        try {
            // Check if this is a new user
            boolean isNewUser = userService.findById(userId).isEmpty();

            User u = new User();
            u.setUserId(userId);
            User saved = userService.createOrUpdateUser(u);

            // If this is a new user, initialize default data
            if (isNewUser) {
                logger.info("Initializing default data for new user: {}", userId);

                // Copy master categories to user expense categories
                try {
                    userExpenseCategoryService.onUserCreated(userId);
                } catch (Exception ex) {
                    logger.warn("Failed to copy master categories for user {}", userId, ex);
                }

                // Copy planned expenses into user's user_expenses based on newly added categories
                try {
                    plannedExpensesService.copyPlannedToUser(userId);
                } catch (Exception ex) {
                    logger.warn("Failed to copy planned expenses for user {}", userId, ex);
                }

                // Create default user preferences for the new user
                try {
                    userPreferencesService.createDefaultsForUser(userId);
                } catch (Exception ex) {
                    logger.warn("Failed to create default preferences for user {}", userId, ex);
                }

                // Generate monthly balance for the new user (previous month)
                try {
                    YearMonth prevMonth = YearMonth.now().minusMonths(1);
                    monthlyBalanceService.generateForUserAndMonth(userId, prevMonth);
                    logger.info("Generated monthly balance for new user {}", userId);
                } catch (Exception ex) {
                    logger.warn("Failed to generate monthly balance for user {}", userId, ex);
                }
            }

            logger.info("User created/updated: userId={}", saved.getUserId());
            return ResponseEntity.ok(Map.of("status", "success", "userId", saved.getUserId()));
        } catch (IllegalArgumentException ex) {
            logger.warn("Validation error during createOrUpdateUser: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        if (userId == null || userId.isBlank()) {
            logger.warn("logout called without userId");
            throw new BadRequestException("userId is required");
        }
        userService.updateLastSeenAt(userId.trim());
        logger.info("User logged out: {}", userId);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        if (userId == null || userId.isBlank()) {
            logger.warn("deleteUser called with empty userId");
            throw new BadRequestException("userId is required");
        }
        var optUser = userService.findById(userId);
        if (optUser.isEmpty()) {
            logger.warn("deleteUser called for non-existing userId={}", userId);
            throw new UserNotFoundException(userId);
        }
        expenseService.deleteAllByUserId(userId);
        incomeService.deleteAllByUserId(userId);
        userExpenseCategoryService.deleteAll(userId);
        userService.deleteUser(userId);
        logger.info("Deleted user and related data for userId={}", userId);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserDetails(@PathVariable String userId) {
        if (userId == null || userId.isBlank()) {
            logger.warn("getUserDetails called without userId");
            throw new BadRequestException("userId is required");
        }
        var opt = userService.findById(userId.trim());
        if (opt.isEmpty()) {
            logger.warn("getUserDetails: user not found userId={}", userId);
            throw new UserNotFoundException(userId);
        }
        User user = opt.get();
        UserResponse resp = new UserResponse();
        resp.setUserId(user.getUserId());
        resp.setStatus(user.getStatus());
        resp.setLastSeenAt(user.getLastSeenAt());
        resp.setCreatedAt(user.getCreatedAt());
        logger.info("getUserDetails successful for userId={}", user.getUserId());
        return ResponseEntity.ok(resp);
    }
}
