package com.expensetracker.controller;

import com.expensetracker.dto.ExpenseDeleteRequest;
import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.model.Expense;
import com.expensetracker.service.ExpenseCategoryService;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.UserService;
import com.expensetracker.validator.RequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expense")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final RequestValidator requestValidator;
    private final UserService userService;
    private final ExpenseCategoryService expenseCategoryService;

    @Autowired
    public ExpenseController(ExpenseService expenseService,
                             RequestValidator requestValidator,
                             UserService userService,
                             ExpenseCategoryService expenseCategoryService) {
        this.expenseService = expenseService;
        this.requestValidator = requestValidator;
        this.userService = userService;
        this.expenseCategoryService = expenseCategoryService;
    }

    @PostMapping("/all")
    public ResponseEntity<?> getExpensesByUser(@RequestBody ExpenseRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username is required in the request body"));
        }
        List<com.expensetracker.dto.ExpenseResponse> expenses = expenseService.getExpenseResponsesByUsername(request.getUsername());
        return ResponseEntity.ok(expenses);
    }

    @PostMapping("/range")
    public ResponseEntity<?> getExpensesByRange(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String startStr = body.get("start");
        String endStr = body.get("end");
        if (username == null || username.isBlank() || startStr == null || endStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username, start and end (YYYY-MM-DD) required"));
        }
        LocalDate start = LocalDate.parse(startStr);
        LocalDate end = LocalDate.parse(endStr);
        return ResponseEntity.ok(expenseService.getExpenseResponsesByUsernameAndDateRange(username, start, end));
    }

    @PostMapping("/month")
    public ResponseEntity<?> getExpensesForMonth(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        Integer year = (Integer) body.get("year");
        Integer month = (Integer) body.get("month");
        if (username == null || username.isBlank() || year == null || month == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username, year and month required"));
        }
        return ResponseEntity.ok(expenseService.getExpenseResponsesByUsernameForMonth(username, year, month));
    }

    @PostMapping("/year")
    public ResponseEntity<?> getExpensesForYear(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        Integer year = (Integer) body.get("year");
        if (username == null || username.isBlank() || year == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and year required"));
        }
        return ResponseEntity.ok(expenseService.getExpenseResponsesByUsernameForYear(username, year));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addExpense(@RequestBody ExpenseRequest request) {
        boolean safe = requestValidator.validateInsertRequest(request);
        if (!safe) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid request"));
        }
        if (userService.findByUsername(request.getUsername()).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "user does not exist"));
        }
        if (expenseCategoryService.findById(request.getExpenseCategoryId()).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "expense category does not exist"));
        }
        Expense saved = expenseService.addExpense(request);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteExpense(@RequestBody ExpenseDeleteRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
        }
        if (request.getExpensesId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "expensesId is required"));
        }

        boolean deleted = expenseService.deleteExpense(request.getUsername(), request.getExpensesId());
        if (!deleted) {
            return ResponseEntity.status(404).body(Map.of("error", "expense not found or does not belong to user"));
        }
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateExpense(@RequestBody ExpenseRequest request) {
        if (request == null || request.getExpensesId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "expensesId required for update"));
        }
        if (request.getUsername() != null && userService.findByUsername(request.getUsername()).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "user does not exist"));
        }
        try {
            com.expensetracker.model.Expense updated = expenseService.updateExpense(request);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
