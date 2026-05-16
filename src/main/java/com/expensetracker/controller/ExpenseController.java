package com.expensetracker.controller;

import com.expensetracker.dto.BankStatementImportResult;
import com.expensetracker.dto.ExpenseDeleteRequest;
import com.expensetracker.dto.ExpensePageRequest;
import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.ExpenseCategoryNotFoundException;
import com.expensetracker.exception.ExpenseNotFoundException;
import com.expensetracker.exception.UserNotFoundException;
import com.expensetracker.service.BankStatementImportService;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.UserExpenseCategoryService;
import com.expensetracker.service.UserService;
import com.expensetracker.util.Constants;
import com.expensetracker.validator.RequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/expense")
public class ExpenseController {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);

    private final ExpenseService expenseService;
    private final RequestValidator requestValidator;
    private final UserService userService;
    private final UserExpenseCategoryService userExpenseCategoryService;
    private final BankStatementImportService bankStatementImportService;

    @Autowired
    public ExpenseController(ExpenseService expenseService,
                             RequestValidator requestValidator,
                             UserService userService,
                             UserExpenseCategoryService userExpenseCategoryService,
                             BankStatementImportService bankStatementImportService) {
        this.expenseService = expenseService;
        this.requestValidator = requestValidator;
        this.userService = userService;
        this.userExpenseCategoryService = userExpenseCategoryService;
        this.bankStatementImportService = bankStatementImportService;
    }

    @PostMapping("/all")
    public ResponseEntity<?> getExpensesByUser(@RequestBody ExpensePageRequest req) {
        logger.debug("getExpensesByUser called with request: {}", req);
        if (req == null || req.getUserId() == null || req.getUserId().isBlank()) {
            throw new BadRequestException("userId is required in the request body");
        }
        int size = req.getSize() != null ? req.getSize() : 10;
        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("Invalid page size. Allowed values: 10, 20, 50, 100");
        }
        req.setSize(size);
        var pageResp = expenseService.getFilteredExpenses(req.getUserId(), null, null, req);
        logger.info("Retrieved {} expenses for userId: {}", pageResp.getTotalElements(), req.getUserId());
        return ResponseEntity.ok(Map.of(
                "content", pageResp.getContent(),
                "page", pageResp.getNumber(),
                "size", pageResp.getSize(),
                "totalPages", pageResp.getTotalPages(),
                "totalElements", pageResp.getTotalElements()
        ));
    }

    @PostMapping("/range")
    public ResponseEntity<?> getExpensesByRange(@RequestBody ExpensePageRequest req) {
        logger.debug("getExpensesByRange called with request: {}", req);
        if (req == null || req.getUserId() == null || req.getUserId().isBlank()
                || req.getStart() == null || req.getEnd() == null) {
            throw new BadRequestException("userId, start and end (YYYY-MM-DD) are required");
        }
        int size = req.getSize() != null ? req.getSize() : 10;
        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("Invalid page size. Allowed values: 10, 20, 50, 100");
        }
        req.setSize(size);
        LocalDate start = LocalDate.parse(req.getStart());
        LocalDate end   = LocalDate.parse(req.getEnd());
        var pageResp = expenseService.getFilteredExpenses(req.getUserId(), start, end, req);
        logger.info("Retrieved {} expenses for userId: {} in range {} to {}",
                pageResp.getTotalElements(), req.getUserId(), start, end);
        return ResponseEntity.ok(Map.of(
                "content", pageResp.getContent(),
                "page", pageResp.getNumber(),
                "size", pageResp.getSize(),
                "totalPages", pageResp.getTotalPages(),
                "totalElements", pageResp.getTotalElements()
        ));
    }

    @PostMapping("/month")
    public ResponseEntity<?> getExpensesForMonth(@RequestBody ExpensePageRequest req) {
        logger.debug("getExpensesForMonth called with request: {}", req);
        if (req == null || req.getUserId() == null || req.getUserId().isBlank()
                || req.getYear() == null || req.getMonth() == null) {
            throw new BadRequestException("userId, year and month are required");
        }
        int size = req.getSize() != null ? req.getSize() : 10;
        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("Invalid page size. Allowed values: 10, 20, 50, 100");
        }
        req.setSize(size);
        java.time.YearMonth ym = java.time.YearMonth.of(req.getYear(), req.getMonth());
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();
        var resp = expenseService.getFilteredExpenses(req.getUserId(), start, end, req);
        logger.info("Retrieved {} expenses for userId: {} for {}/{}",
                resp.getTotalElements(), req.getUserId(), req.getYear(), req.getMonth());
        return ResponseEntity.ok(Map.of(
                "content", resp.getContent(),
                "page", resp.getNumber(),
                "size", resp.getSize(),
                "totalPages", resp.getTotalPages(),
                "totalElements", resp.getTotalElements()
        ));
    }

    @PostMapping("/year")
    public ResponseEntity<?> getExpensesForYear(@RequestBody ExpensePageRequest req) {
        logger.debug("getExpensesForYear called with request: {}", req);
        if (req == null || req.getUserId() == null || req.getUserId().isBlank() || req.getYear() == null) {
            throw new BadRequestException("userId and year are required");
        }
        int size = req.getSize() != null ? req.getSize() : 10;
        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("Invalid page size. Allowed values: 10, 20, 50, 100");
        }
        req.setSize(size);
        LocalDate start = LocalDate.of(req.getYear(), 1, 1);
        LocalDate end   = LocalDate.of(req.getYear(), 12, 31);
        var resp = expenseService.getFilteredExpenses(req.getUserId(), start, end, req);
        logger.info("Retrieved {} expenses for userId: {} for year {}",
                resp.getTotalElements(), req.getUserId(), req.getYear());
        return ResponseEntity.ok(Map.of(
                "content", resp.getContent(),
                "page", resp.getNumber(),
                "size", resp.getSize(),
                "totalPages", resp.getTotalPages(),
                "totalElements", resp.getTotalElements()
        ));
    }

    @PostMapping("/total/month")
    public ResponseEntity<?> getTotalExpensesForMonth(@RequestBody Map<String, Object> body) {
        logger.debug("getTotalExpensesForMonth called with body: {}", body);
        String userId = (String) body.get("userId");
        Integer year = (Integer) body.get("year");
        Integer month = (Integer) body.get("month");
        if (userId == null || userId.isBlank() || year == null || month == null) {
            throw new BadRequestException("userId, year and month are required");
        }
        java.math.BigDecimal total = expenseService.getTotalExpenseAmountForMonth(userId, year, month);
        logger.info("Total expenses for userId={} for {}/{}: {}", userId, year, month, total);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "year", year,
                "month", month,
                "totalAmount", total
        ));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addExpense(@RequestBody ExpenseRequest request) {
        logger.debug("addExpense called with request: {}", request);

        // Validate request - throws exceptions if validation fails
        requestValidator.validateInsertRequest(request);

        // Check if user exists
        if (userService.findById(request.getUserId()).isEmpty()) {
            throw new UserNotFoundException(request.getUserId());
        }

        // Check if expense category exists
        if (userExpenseCategoryService.findById(request.getUserExpenseCategoryId()).isEmpty()) {
            throw new ExpenseCategoryNotFoundException(request.getUserExpenseCategoryId());
        }

        expenseService.addExpense(request);
        logger.info("Expense added successfully for userId: {}", request.getUserId());
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteExpense(@RequestBody ExpenseDeleteRequest request) {
        logger.debug("deleteExpense called with request: {}", request);
        if (request == null || request.getUserId() == null || request.getUserId().isBlank()) {
            throw new BadRequestException("userId is required");
        }
        if (request.getExpensesId() == null) {
            throw new BadRequestException("expensesId is required");
        }

        boolean deleted = expenseService.deleteExpense(request.getUserId(), request.getExpensesId());
        if (!deleted) {
            throw new ExpenseNotFoundException("Expense not found or userId mismatch for expenseId: " + request.getExpensesId());
        }
        logger.info("Expense deleted successfully: expenseId={}, userId={}", request.getExpensesId(), request.getUserId());
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateExpense(@RequestBody ExpenseRequest request) {
        logger.debug("updateExpense called with request: {}", request);

        // Validate request - throws exceptions if validation fails
        requestValidator.validateUpdateRequest(request);

        // Check if user exists (if userId is provided)
        if (request.getUserId() != null && userService.findById(request.getUserId()).isEmpty()) {
            throw new UserNotFoundException(request.getUserId());
        }

        expenseService.updateExpense(request);
        logger.info("Expense updated successfully: expenseId={}", request.getExpensesId());
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    /**
     * Import expenses and incomes from an HDFC Bank account statement PDF.
     *
     * <h3>Request parameters</h3>
     * <ul>
     *   <li>{@code file}             – multipart PDF (required)</li>
     *   <li>{@code userId}           – user id (required)</li>
     *   <li>{@code password}         – explicit PDF password (optional)</li>
     *   <li>{@code useStoredPassword}– {@code true}  → use the password already saved in DB (optional, default false)</li>
     *   <li>{@code storePassword}    – {@code true}  → encrypt and save the supplied password to DB (optional, default false)</li>
     * </ul>
     */
    @PostMapping(value = "/import/hdfc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importHdfcBankStatement(
            @RequestParam("file")                                       MultipartFile file,
            @RequestParam("userId")                                     String userId,
            @RequestParam(value = "password",          required = false) String password,
            @RequestParam(value = "useStoredPassword", defaultValue = "false") boolean useStoredPassword,
            @RequestParam(value = "storePassword",     defaultValue = "false") boolean storePassword) {

        logger.debug("importHdfcBankStatement called: userId={}, useStoredPassword={}, storePassword={}",
                userId, useStoredPassword, storePassword);

        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("userId is required");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A non-empty PDF file is required");
        }
        if (storePassword && (password == null || password.isBlank())) {
            throw new BadRequestException("A password must be provided when storePassword is true");
        }
        if (useStoredPassword && (password != null && !password.isBlank())) {
            throw new BadRequestException(
                    "Provide either 'password' or 'useStoredPassword=true', not both. "
                    + "If you want to replace the stored password, pass the new password with 'storePassword=true'.");
        }

        BankStatementImportResult result = bankStatementImportService
                .importStatement(file, userId, password, useStoredPassword, storePassword);

        logger.info("HDFC import complete for userId={}: expensesAdded={}, incomesAdded={}, skipped={}",
                userId, result.getExpensesAdded(), result.getIncomesAdded(), result.getSkippedCount());

        return ResponseEntity.ok(result);
    }
}
