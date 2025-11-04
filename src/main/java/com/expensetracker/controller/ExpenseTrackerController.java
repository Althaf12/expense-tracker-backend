package com.expensetracker.controller;

import com.expensetracker.dto.*;
import com.expensetracker.model.*;
import com.expensetracker.service.*;
import com.expensetracker.validator.RequestValidator;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
public class ExpenseTrackerController {

    private final ExpenseService expenseService;
    private final RequestValidator requestValidator;
    private final UserService userService;
    private final IncomeService incomeService;
    private final ExpenseCategoryService expenseCategoryService;

    @Autowired
    public ExpenseTrackerController(ExpenseService expenseService,
                                    RequestValidator requestValidator,
                                    UserService userService,
                                    IncomeService incomeService,
                                    ExpenseCategoryService expenseCategoryService) {
        this.expenseService = expenseService;
        this.requestValidator = requestValidator;
        this.userService = userService;
        this.incomeService = incomeService;
        this.expenseCategoryService = expenseCategoryService;
    }

    @PostMapping("/getMyExpenses")
    public ResponseEntity<?> getExpensesByUser(@RequestBody ExpenseRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required in the request body"));
        }

        List<com.expensetracker.dto.ExpenseResponse> expenses = expenseService.getExpenseResponsesByUserId(request.getUserId());
        return ResponseEntity.ok(expenses);
    }

    @PostMapping("/getMyExpensesByRange")
    public ResponseEntity<?> getExpensesByRange(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String startStr = body.get("start");
        String endStr = body.get("end");
        if (userId == null || userId.isBlank() || startStr == null || endStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId, start and end (YYYY-MM-DD) required"));
        }
        LocalDate start = LocalDate.parse(startStr);
        LocalDate end = LocalDate.parse(endStr);
        return ResponseEntity.ok(expenseService.getExpenseResponsesByUserIdAndDateRange(userId, start, end));
    }

    @PostMapping("/getMyExpensesForMonth")
    public ResponseEntity<?> getExpensesForMonth(@RequestBody Map<String, Object> body) {
        String userId = (String) body.get("userId");
        Integer year = (Integer) body.get("year");
        Integer month = (Integer) body.get("month");
        if (userId == null || userId.isBlank() || year == null || month == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId, year and month required"));
        }
        return ResponseEntity.ok(expenseService.getExpenseResponsesByUserIdForMonth(userId, year, month));
    }

    @PostMapping("/getMyExpensesForYear")
    public ResponseEntity<?> getExpensesForYear(@RequestBody Map<String, Object> body) {
        String userId = (String) body.get("userId");
        Integer year = (Integer) body.get("year");
        if (userId == null || userId.isBlank() || year == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId and year required"));
        }
        return ResponseEntity.ok(expenseService.getExpenseResponsesByUserIdForYear(userId, year));
    }

    @PostMapping("/addExpense")
    public ResponseEntity<?> addExpense(@RequestBody ExpenseRequest request) {
        // Validate request
        boolean safe = requestValidator.validateInsertRequest(request);
        if (!safe) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid request"));
        }
        // ensure user exists
        if (userService.findById(request.getUserId()).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "user does not exist"));
        }
        // ensure expense category exists
        if (expenseCategoryService.findById(request.getExpenseCategoryId()).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "expense category does not exist"));
        }
        Expense saved = expenseService.addExpense(request);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/deleteExpense")
    public ResponseEntity<?> deleteExpense(@RequestBody ExpenseDeleteRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
        }
        if (request.getExpensesId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "expensesId is required"));
        }

        boolean deleted = expenseService.deleteExpense(request.getUserId(), request.getExpensesId());
        if (!deleted) {
            return ResponseEntity.status(404).body(Map.of("error", "expense not found or does not belong to user"));
        }
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    // --- Users ---
    @PostMapping("/user")
    public ResponseEntity<?> createOrUpdateUser(@RequestBody UserRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "request required"));
        }
        // Email validation and password rules are handled by service; build User object
        User u = new User();
        // don't set userId from request; service will generate if absent
        u.setUsername(request.getUsername());
        u.setEmail(request.getEmail());
        u.setPassword(request.getPassword());
        try {
            User saved = userService.createOrUpdateUser(u);
            // Do not return password in response
            saved.setPassword(null);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // Don't integrate with frontend; admin only
    @DeleteMapping("/user/{userId}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        if (userService.findById(userId).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "user does not exist"));
        }
        // delete related expenses and income first to maintain referential integrity
        List<Expense> expenses = expenseService.getExpensesByUserId(userId);
        for (Expense e : expenses) {
            expenseService.deleteExpense(userId, e.getExpensesId());
        }
        List<com.expensetracker.model.Income> incomes = incomeService.getByUser(userId);
        for (com.expensetracker.model.Income inc : incomes) {
            incomeService.deleteIncome(inc.getIncomeId());
        }
        userService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    // --- Income endpoints ---
    @PostMapping("/income")
    public ResponseEntity<?> addIncome(@RequestBody IncomeRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        if (userService.findByUsername(request.getUsername()).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "user does not exist"));
        }
        com.expensetracker.model.Income inc = new com.expensetracker.model.Income();
        inc.setUsername(request.getUsername());
        inc.setSource(request.getSource() == null ? "Salary" : request.getSource());
        inc.setAmount(request.getAmount());
        inc.setReceivedDate(request.getReceivedDate());
        inc.setMonth(request.getMonth());
        inc.setYear(request.getYear());
        com.expensetracker.model.Income saved = incomeService.addIncome(inc);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/income/range")
    public ResponseEntity<?> incomesByRange(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String startStr = body.get("start");
        String endStr = body.get("end");
        if (userId == null || userId.isBlank() || startStr == null || endStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId, start and end (YYYY-MM-DD) required"));
        }
        LocalDate start = LocalDate.parse(startStr);
        LocalDate end = LocalDate.parse(endStr);
        return ResponseEntity.ok(incomeService.getByUserAndDateRange(userId, start, end));
    }

    // --- Expense categories ---
    @PostMapping("/expenseCategory")
    public ResponseEntity<?> addExpenseCategory(@RequestBody ExpenseCategoryRequest request) {
        if (request == null || request.getExpenseCategoryName() == null || request.getExpenseCategoryName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "expenseCategoryName required"));
        }
        com.expensetracker.model.ExpenseCategory c = new com.expensetracker.model.ExpenseCategory();
        c.setExpenseCategoryName(request.getExpenseCategoryName());
        com.expensetracker.model.ExpenseCategory saved = expenseCategoryService.addOrUpdate(c);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/expenseCategory/{id}")
    public ResponseEntity<?> deleteExpenseCategory(@PathVariable Integer id) {
        expenseCategoryService.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    // --- Update endpoints ---
    @PutMapping("/updateExpense")
    public ResponseEntity<?> updateExpense(@RequestBody ExpenseRequest request) {
        if (request == null || request.getExpensesId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "expensesId required for update"));
        }
        // ensure user exists
        if (request.getUserId() != null && userService.findById(request.getUserId()).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "user does not exist"));
        }

        // Also ensure atleast one field to update is passed

        try {
            com.expensetracker.model.Expense updated = expenseService.updateExpense(request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/income/{incomeId}")
    public ResponseEntity<?> updateIncome(@PathVariable Integer incomeId, @RequestBody IncomeRequest request) {
        if (incomeId == null) return ResponseEntity.badRequest().body(Map.of("error", "incomeId required"));
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        if (userService.findByUsername(request.getUsername()).isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "user does not exist"));
        com.expensetracker.model.Income upd = new com.expensetracker.model.Income();
        upd.setUsername(request.getUsername());
        upd.setSource(request.getSource());
        upd.setAmount(request.getAmount());
        upd.setReceivedDate(request.getReceivedDate());
        upd.setMonth(request.getMonth());
        upd.setYear(request.getYear());
        try {
            com.expensetracker.model.Income saved = incomeService.updateIncome(incomeId, request.getUsername(), upd);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/user/password")
    public ResponseEntity<?> updateUserPassword(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String username = body.get("username");
        String email = body.get("email");
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "newPassword required"));
        try {
            if (userId != null && !userId.isBlank()) {
                return ResponseEntity.ok(userService.updatePasswordByUserId(userId, newPassword));
            } else if (username != null && !username.isBlank()) {
                return ResponseEntity.ok(userService.updatePasswordByUsername(username, newPassword));
            } else if (email != null && !email.isBlank()) {
                return ResponseEntity.ok(userService.updatePasswordByEmail(email, newPassword));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "provide userId or username or email"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/expenseCategory")
    public ResponseEntity<?> updateExpenseCategory(@RequestBody Map<String, String> body) {
        String idStr = body.get("expenseCategoryId");
        String name = body.get("expenseCategoryName");
        String newName = body.get("newName");
        if ((idStr == null || idStr.isBlank()) && (name == null || name.isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "provide expenseCategoryId or expenseCategoryName to identify category"));
        }
        if (newName == null || newName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "newName required"));
        }
        try {
            if (idStr != null && !idStr.isBlank()) {
                Integer id = Integer.parseInt(idStr);
                return ResponseEntity.ok(expenseCategoryService.updateById(id, newName));
            } else {
                return ResponseEntity.ok(expenseCategoryService.updateByName(name, newName));
            }
        } catch (NumberFormatException nfe) {
            return ResponseEntity.badRequest().body(Map.of("error", "expenseCategoryId must be integer"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    // --- User details ---
    @PostMapping("/user/details")
    public ResponseEntity<?> getUserDetails(@RequestBody Map<String, String> body) {
        if (body == null) return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        String username = body.get("username");
        String email = body.get("email");
        if ((username == null || username.isBlank()) && (email == null || email.isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "provide username or email"));
        }
        try {
            com.expensetracker.model.User user;
            if (username != null && !username.isBlank()) {
                var opt = userService.findByUsername(username);
                if (opt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "user not found"));
                user = opt.get();
            } else {
                var opt = userService.findByEmail(email);
                if (opt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "user not found"));
                user = opt.get();
            }
            // map to DTO to avoid exposing password
            com.expensetracker.dto.UserResponse resp = new com.expensetracker.dto.UserResponse();
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

    @PostMapping("/user/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        if (body == null) return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        String usernameOrEmail = body.get("username") != null ? body.get("username") : body.get("email");
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "provide username or email"));
        try {
            String token = userService.generatePasswordResetToken(usernameOrEmail);
            // do not return token in production; return only status. For debugging we can return token.
            return ResponseEntity.ok(Map.of("status", "reset email sent", "token", token));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @PostMapping("/user/reset-password")
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

}
