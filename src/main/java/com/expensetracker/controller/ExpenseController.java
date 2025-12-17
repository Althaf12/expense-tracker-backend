package com.expensetracker.controller;

import com.expensetracker.dto.ExpenseDeleteRequest;
import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.service.ExpenseCategoryService;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.UserExpenseCategoryService;
import com.expensetracker.service.UserService;
import com.expensetracker.validator.RequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import java.util.Set;

@RestController
@RequestMapping("/api/expense")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final RequestValidator requestValidator;
    private final UserService userService;
    private final ExpenseCategoryService expenseCategoryService;
    private final UserExpenseCategoryService userExpenseCategoryService;
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10,20,50,100);

    @Autowired
    public ExpenseController(ExpenseService expenseService,
                             RequestValidator requestValidator,
                             UserService userService,
                             ExpenseCategoryService expenseCategoryService, UserExpenseCategoryService userExpenseCategoryService) {
        this.expenseService = expenseService;
        this.requestValidator = requestValidator;
        this.userService = userService;
        this.expenseCategoryService = expenseCategoryService;
        this.userExpenseCategoryService = userExpenseCategoryService;
    }

    @PostMapping("/all")
    public ResponseEntity<?> getExpensesByUser(@RequestBody Map<String, Object> request) {
        if (request == null || request.get("username") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username is required in the request body"));
        }
        String username = (String) request.get("username");
        int page = request.get("page") instanceof Number ? ((Number) request.get("page")).intValue() : 0;
        int size = request.get("size") instanceof Number ? ((Number) request.get("size")).intValue() : 10;
        if (!ALLOWED_PAGE_SIZES.contains(size)) return ResponseEntity.badRequest().body(Map.of("error", "invalid page size"));
        var pageResp = expenseService.getExpenseResponsesByUsername(username, page, size);
        return ResponseEntity.ok(Map.of(
                "content", pageResp.getContent(),
                "page", pageResp.getNumber(),
                "size", pageResp.getSize(),
                "totalPages", pageResp.getTotalPages(),
                "totalElements", pageResp.getTotalElements()
        ));
    }

    @PostMapping("/range")
    public ResponseEntity<?> getExpensesByRange(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String startStr = body.get("start");
        String endStr = body.get("end");
        int page = body.get("page") != null ? Integer.parseInt(body.get("page")) : 0;
        int size = body.get("size") != null ? Integer.parseInt(body.get("size")) : 10;
        if (username == null || username.isBlank() || startStr == null || endStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username, start and end (YYYY-MM-DD) required"));
        }
        if (!ALLOWED_PAGE_SIZES.contains(size)) return ResponseEntity.badRequest().body(Map.of("error", "invalid page size"));
        LocalDate start = LocalDate.parse(startStr);
        LocalDate end = LocalDate.parse(endStr);
        var pageResp = expenseService.getExpenseResponsesByUsernameAndDateRange(username, start, end, page, size);
        return ResponseEntity.ok(Map.of(
                "content", pageResp.getContent(),
                "page", pageResp.getNumber(),
                "size", pageResp.getSize(),
                "totalPages", pageResp.getTotalPages(),
                "totalElements", pageResp.getTotalElements()
        ));
    }

    @PostMapping("/month")
    public ResponseEntity<?> getExpensesForMonth(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        Integer year = (Integer) body.get("year");
        Integer month = (Integer) body.get("month");
        int page = body.get("page") instanceof Number ? ((Number) body.get("page")).intValue() : 0;
        int size = body.get("size") instanceof Number ? ((Number) body.get("size")).intValue() : 10;
        if (username == null || username.isBlank() || year == null || month == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username, year and month required"));
        }
        if (!ALLOWED_PAGE_SIZES.contains(size)) return ResponseEntity.badRequest().body(Map.of("error", "invalid page size"));
        var resp = expenseService.getExpenseResponsesByUsernameForMonth(username, year, month, page, size);
        return ResponseEntity.ok(Map.of(
                "content", resp.getContent(),
                "page", resp.getNumber(),
                "size", resp.getSize(),
                "totalPages", resp.getTotalPages(),
                "totalElements", resp.getTotalElements()
        ));
    }

    @PostMapping("/year")
    public ResponseEntity<?> getExpensesForYear(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        Integer year = (Integer) body.get("year");
        int page = body.get("page") instanceof Number ? ((Number) body.get("page")).intValue() : 0;
        int size = body.get("size") instanceof Number ? ((Number) body.get("size")).intValue() : 10;
        if (username == null || username.isBlank() || year == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and year required"));
        }
        if (!ALLOWED_PAGE_SIZES.contains(size)) return ResponseEntity.badRequest().body(Map.of("error", "invalid page size"));
        var resp = expenseService.getExpenseResponsesByUsernameForYear(username, year, page, size);
        return ResponseEntity.ok(Map.of(
                "content", resp.getContent(),
                "page", resp.getNumber(),
                "size", resp.getSize(),
                "totalPages", resp.getTotalPages(),
                "totalElements", resp.getTotalElements()
        ));
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
        if (userExpenseCategoryService.findById(request.getUserExpenseCategoryId()).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "expense category does not exist"));
        }
        expenseService.addExpense(request);
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
