package com.expensetracker.controller;

import com.expensetracker.dto.MonthlyBalanceUpdateRequest;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.model.MonthlyBalance;
import com.expensetracker.service.MonthlyBalanceService;
import com.expensetracker.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/api/monthly-balance")
public class MonthlyBalanceController {

    private static final Logger logger = LoggerFactory.getLogger(MonthlyBalanceController.class);

    private final MonthlyBalanceService monthlyBalanceService;

    public MonthlyBalanceController(MonthlyBalanceService monthlyBalanceService) {
        this.monthlyBalanceService = monthlyBalanceService;
    }

    /**
     * Get all monthly balances for a user (paginated).
     * Example: GET /api/monthly-balance/all?userId=abc123&page=0&size=10
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllMonthlyBalances(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.info("API getAllMonthlyBalances called for userId={}, page={}, size={}", userId, page, size);

        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("userId is required");
        }

        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("Invalid page size. Allowed values: " + Constants.ALLOWED_PAGE_SIZES);
        }

        Page<MonthlyBalance> balancesPage = monthlyBalanceService.findAllByUserId(userId.trim(), page, size);

        logger.info("Retrieved {} monthly balances for userId={}", balancesPage.getTotalElements(), userId);

        return ResponseEntity.ok(Map.of(
                "content", balancesPage.getContent(),
                "page", balancesPage.getNumber(),
                "size", balancesPage.getSize(),
                "totalPages", balancesPage.getTotalPages(),
                "totalElements", balancesPage.getTotalElements()
        ));
    }

    /**
     * Update a specific monthly balance (opening or closing balance).
     * Example: PUT /api/monthly-balance/update
     * Body: { "userId": "abc123", "year": 2025, "month": 12, "openingBalance": 5000.0, "closingBalance": 4500.0 }
     */
    @PutMapping("/update")
    public ResponseEntity<?> updateMonthlyBalance(@RequestBody MonthlyBalanceUpdateRequest request) {
        logger.info("API updateMonthlyBalance called for userId={}, year={}, month={}",
                request.getUserId(), request.getYear(), request.getMonth());

        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new BadRequestException("userId is required");
        }
        if (request.getYear() == null) {
            throw new BadRequestException("year is required");
        }
        if (request.getMonth() == null) {
            throw new BadRequestException("month is required");
        }
        if (request.getOpeningBalance() == null && request.getClosingBalance() == null) {
            throw new BadRequestException("At least one of openingBalance or closingBalance must be provided");
        }

        MonthlyBalance updated = monthlyBalanceService.updateMonthlyBalance(
                request.getUserId().trim(),
                request.getYear(),
                request.getMonth(),
                request.getOpeningBalance(),
                request.getClosingBalance()
        );

        logger.info("Updated monthly balance for userId={}, year={}, month={}",
                request.getUserId(), request.getYear(), request.getMonth());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", updated
        ));
    }

    /**
     * Trigger generation of monthly balances for all users.
     * If year/month are omitted, defaults to the previous month.
     */
    @PostMapping("/generate")
    public ResponseEntity<String> generateForMonth(@RequestParam(required = false) Integer year,
                                                   @RequestParam(required = false) Integer month) {
        YearMonth target;
        if (year == null || month == null) {
            target = YearMonth.now().minusMonths(1);
        } else {
            target = YearMonth.of(year, month);
        }

        logger.info("API generateForMonth called for all users for {}-{}", target.getYear(), target.getMonthValue());
        monthlyBalanceService.generateForAllUsersAndMonth(target);
        String msg = String.format("Monthly balances generated for %d-%02d", target.getYear(), target.getMonthValue());
        return ResponseEntity.ok(msg);
    }

    /**
     * Trigger generation for a single user for a given month. If year/month omitted, defaults to previous month.
     * Example: POST /api/monthly-balance/generate/{userId}?year=2025&month=12
     */
    @PostMapping("/generate/{userId}")
    public ResponseEntity<String> generateForUser(@PathVariable String userId,
                                                  @RequestParam(required = false) Integer year,
                                                  @RequestParam(required = false) Integer month) {
        YearMonth target;
        if (year == null || month == null) {
            target = YearMonth.now().minusMonths(1);
        } else {
            target = YearMonth.of(year, month);
        }

        logger.info("API generateForUser called for userId={} for {}-{}", userId, target.getYear(), target.getMonthValue());
        monthlyBalanceService.generateForUserAndMonth(userId, target);
        String msg = String.format("Monthly balance generated for user %s for %d-%02d", userId, target.getYear(), target.getMonthValue());
        return ResponseEntity.ok(msg);
    }

    /**
     * Fetch the MonthlyBalance snapshot for the previous month for a given user.
     * Example: GET /api/monthly-balance/previous?userId=abc123
     */
    @GetMapping("/previous")
    public ResponseEntity<?> getPreviousMonthBalance(@RequestParam String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("userId is required");
        }

        YearMonth previous = YearMonth.now().minusMonths(1);
        logger.info("API getPreviousMonthBalance called for userId={} for {}-{}", userId, previous.getYear(), previous.getMonthValue());
        var mbOpt = monthlyBalanceService.findByUserIdYearMonth(userId.trim(), previous.getYear(), previous.getMonthValue());
        return mbOpt.map(mb -> ResponseEntity.<Object>ok(mb))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("No monthly balance found for user for previous month"));
    }
}
