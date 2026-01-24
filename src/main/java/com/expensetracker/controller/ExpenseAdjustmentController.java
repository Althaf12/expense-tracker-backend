package com.expensetracker.controller;

import com.expensetracker.dto.ExpenseAdjustmentRequest;
import com.expensetracker.dto.ExpenseAdjustmentResponse;
import com.expensetracker.service.ExpenseAdjustmentService;
import com.expensetracker.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing expense adjustments (refunds, cashbacks, reversals).
 */
@RestController
@RequestMapping("/api/expense-adjustment")
public class ExpenseAdjustmentController {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseAdjustmentController.class);

    private final ExpenseAdjustmentService adjustmentService;

    public ExpenseAdjustmentController(ExpenseAdjustmentService adjustmentService) {
        this.adjustmentService = adjustmentService;
    }

    /**
     * Create a new expense adjustment.
     *
     * POST /api/expense-adjustment
     */
    @PostMapping
    public ResponseEntity<ExpenseAdjustmentResponse> createAdjustment(
            @RequestBody ExpenseAdjustmentRequest request) {
        logger.info("Creating expense adjustment for userId: {}, expenseId: {}",
                request.getUserId(), request.getExpensesId());
        ExpenseAdjustmentResponse response = adjustmentService.createAdjustment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Update an existing expense adjustment.
     *
     * PUT /api/expense-adjustment
     */
    @PutMapping
    public ResponseEntity<ExpenseAdjustmentResponse> updateAdjustment(
            @RequestBody ExpenseAdjustmentRequest request) {
        logger.info("Updating expense adjustment ID: {}", request.getExpenseAdjustmentsId());
        ExpenseAdjustmentResponse response = adjustmentService.updateAdjustment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an expense adjustment.
     *
     * DELETE /api/expense-adjustment/{userId}/{adjustmentId}
     */
    @DeleteMapping("/{userId}/{adjustmentId}")
    public ResponseEntity<Map<String, String>> deleteAdjustment(
            @PathVariable String userId,
            @PathVariable Integer adjustmentId) {
        logger.info("Deleting expense adjustment ID: {} for userId: {}", adjustmentId, userId);
        adjustmentService.deleteAdjustment(userId, adjustmentId);
        return ResponseEntity.ok(Map.of("message", "Expense adjustment deleted successfully"));
    }

    /**
     * Get a specific adjustment by ID.
     *
     * GET /api/expense-adjustment/{userId}/{adjustmentId}
     */
    @GetMapping("/{userId}/{adjustmentId}")
    public ResponseEntity<ExpenseAdjustmentResponse> getAdjustmentById(
            @PathVariable String userId,
            @PathVariable Integer adjustmentId) {
        logger.info("Fetching expense adjustment ID: {} for userId: {}", adjustmentId, userId);
        ExpenseAdjustmentResponse response = adjustmentService.getAdjustmentById(userId, adjustmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all adjustments for a user with pagination.
     *
     * GET /api/expense-adjustment/user/{userId}?page=0&size=10
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ExpenseAdjustmentResponse>> getAdjustmentsByUserId(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("Fetching adjustments for userId: {}, page: {}, size: {}", userId, page, size);
        Page<ExpenseAdjustmentResponse> response = adjustmentService.getAdjustmentsByUserId(userId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all adjustments for a specific expense.
     *
     * GET /api/expense-adjustment/expense/{userId}/{expenseId}
     */
    @GetMapping("/expense/{userId}/{expenseId}")
    public ResponseEntity<List<ExpenseAdjustmentResponse>> getAdjustmentsByExpenseId(
            @PathVariable String userId,
            @PathVariable Integer expenseId) {
        logger.info("Fetching adjustments for expenseId: {} by userId: {}", expenseId, userId);
        List<ExpenseAdjustmentResponse> response = adjustmentService.getAdjustmentsByExpenseId(userId, expenseId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get adjustments for a user within a date range with pagination.
     *
     * POST /api/expense-adjustment/range
     * Request body: { "userId": "...", "startDate": "yyyy-MM-dd", "endDate": "yyyy-MM-dd" }
     */
    @PostMapping("/range")
    public ResponseEntity<Page<ExpenseAdjustmentResponse>> getAdjustmentsByDateRange(
            @RequestBody Map<String, Object> requestBody,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = (String) requestBody.get("userId");
        LocalDate startDate = LocalDate.parse((String) requestBody.get("startDate"));
        LocalDate endDate = LocalDate.parse((String) requestBody.get("endDate"));

        logger.info("Fetching adjustments for userId: {} from {} to {}", userId, startDate, endDate);
        Page<ExpenseAdjustmentResponse> response = adjustmentService
                .getAdjustmentsByUserIdAndDateRange(userId, startDate, endDate, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get total completed adjustment amount for a specific expense.
     *
     * GET /api/expense-adjustment/total/expense/{expenseId}
     */
    @GetMapping("/total/expense/{expenseId}")
    public ResponseEntity<Map<String, BigDecimal>> getTotalAdjustmentForExpense(
            @PathVariable Integer expenseId) {
        logger.info("Fetching total completed adjustment for expenseId: {}", expenseId);
        BigDecimal total = adjustmentService.getTotalCompletedAdjustmentForExpense(expenseId);
        return ResponseEntity.ok(Map.of("totalAdjustment", total));
    }

    /**
     * Get total completed adjustments for a user in a specific month.
     *
     * GET /api/expense-adjustment/total/month/{userId}/{year}/{month}
     */
    @GetMapping("/total/month/{userId}/{year}/{month}")
    public ResponseEntity<Map<String, BigDecimal>> getTotalAdjustmentForMonth(
            @PathVariable String userId,
            @PathVariable int year,
            @PathVariable int month) {
        logger.info("Fetching total completed adjustments for userId: {} year: {} month: {}",
                userId, year, month);
        BigDecimal total = adjustmentService.getTotalCompletedAdjustmentsForMonth(userId, year, month);
        return ResponseEntity.ok(Map.of("totalAdjustment", total));
    }

    /**
     * Get allowed page sizes.
     *
     * GET /api/expense-adjustment/page-sizes
     */
    @GetMapping("/page-sizes")
    public ResponseEntity<Map<String, Object>> getAllowedPageSizes() {
        return ResponseEntity.ok(Map.of("allowedPageSizes", Constants.ALLOWED_PAGE_SIZES));
    }
}
