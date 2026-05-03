package com.expensetracker.controller;

import com.expensetracker.dto.BankStatementImportResult;
import com.expensetracker.dto.ExpenseDeleteRequest;
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
    public ResponseEntity<?> getExpensesByUser(@RequestBody Map<String, Object> request) {
        logger.debug("getExpensesByUser called with request: {}", request);
        if (request == null || request.get("userId") == null) {
            throw new BadRequestException("userId is required in the request body");
        }
        String userId = (String) request.get("userId");
        int page = request.get("page") instanceof Number ? ((Number) request.get("page")).intValue() : 0;
        int size = request.get("size") instanceof Number ? ((Number) request.get("size")).intValue() : 10;
        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("Invalid page size. Allowed values: 10, 20, 50, 100");
        }
        var pageResp = expenseService.getExpenseResponsesByUserId(userId, page, size);
        logger.info("Retrieved {} expenses for userId: {}", pageResp.getTotalElements(), userId);
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
        logger.debug("getExpensesByRange called with body: {}", body);
        String userId = body.get("userId");
        String startStr = body.get("start");
        String endStr = body.get("end");
        int page = body.get("page") != null ? Integer.parseInt(body.get("page")) : 0;
        int size = body.get("size") != null ? Integer.parseInt(body.get("size")) : 10;
        if (userId == null || userId.isBlank() || startStr == null || endStr == null) {
            throw new BadRequestException("userId, start and end (YYYY-MM-DD) are required");
        }
        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("Invalid page size. Allowed values: 10, 20, 50, 100");
        }
        LocalDate start = LocalDate.parse(startStr);
        LocalDate end = LocalDate.parse(endStr);
        var pageResp = expenseService.getExpenseResponsesByUserIdAndDateRange(userId, start, end, page, size);
        logger.info("Retrieved {} expenses for userId: {} in range {} to {}", pageResp.getTotalElements(), userId, start, end);
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
        logger.debug("getExpensesForMonth called with body: {}", body);
        String userId = (String) body.get("userId");
        Integer year = (Integer) body.get("year");
        Integer month = (Integer) body.get("month");
        int page = body.get("page") instanceof Number ? ((Number) body.get("page")).intValue() : 0;
        int size = body.get("size") instanceof Number ? ((Number) body.get("size")).intValue() : 10;
        if (userId == null || userId.isBlank() || year == null || month == null) {
            throw new BadRequestException("userId, year and month are required");
        }
        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("Invalid page size. Allowed values: 10, 20, 50, 100");
        }
        var resp = expenseService.getExpenseResponsesByUserIdForMonth(userId, year, month, page, size);
        logger.info("Retrieved {} expenses for userId: {} for {}/{}", resp.getTotalElements(), userId, year, month);
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
        logger.debug("getExpensesForYear called with body: {}", body);
        String userId = (String) body.get("userId");
        Integer year = (Integer) body.get("year");
        int page = body.get("page") instanceof Number ? ((Number) body.get("page")).intValue() : 0;
        int size = body.get("size") instanceof Number ? ((Number) body.get("size")).intValue() : 10;
        if (userId == null || userId.isBlank() || year == null) {
            throw new BadRequestException("userId and year are required");
        }
        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("Invalid page size. Allowed values: 10, 20, 50, 100");
        }
        var resp = expenseService.getExpenseResponsesByUserIdForYear(userId, year, page, size);
        logger.info("Retrieved {} expenses for userId: {} for year {}", resp.getTotalElements(), userId, year);
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
     * <p>The endpoint:
     * <ol>
     *   <li>Unlocks the PDF with the supplied password (if any).</li>
     *   <li>Parses all transactions from the HDFC statement table.</li>
     *   <li>Matches the user's current closing balance to a transaction row in the statement.</li>
     *   <li>Adds every subsequent withdrawal as an {@link com.expensetracker.model.Expense}
     *       under the <em>House Expenses</em> category (or the first available active category
     *       if that category does not exist).</li>
     *   <li>Adds every subsequent deposit as an {@link com.expensetracker.model.Income}.</li>
     * </ol>
     *
     * @param file     Multipart PDF file (required)
     * @param userId   Identifier of the user (required)
     * @param password PDF password – leave blank if the file is not password-protected (optional)
     */
    @PostMapping(value = "/import/hdfc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importHdfcBankStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam(value = "password", required = false) String password) {

        logger.debug("importHdfcBankStatement called: userId={}", userId);

        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("userId is required");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A non-empty PDF file is required");
        }

        BankStatementImportResult result = bankStatementImportService
                .importStatement(file, userId, password);

        logger.info("HDFC import complete for userId={}: expensesAdded={}, incomesAdded={}, skipped={}",
                userId, result.getExpensesAdded(), result.getIncomesAdded(), result.getSkippedCount());

        return ResponseEntity.ok(result);
    }
}
