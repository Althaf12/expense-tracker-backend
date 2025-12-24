package com.expensetracker.controller;

import com.expensetracker.service.MonthlyBalanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/monthly-balance")
public class MonthlyBalanceController {

    private static final Logger logger = LoggerFactory.getLogger(MonthlyBalanceController.class);

    private final MonthlyBalanceService monthlyBalanceService;

    public MonthlyBalanceController(MonthlyBalanceService monthlyBalanceService) {
        this.monthlyBalanceService = monthlyBalanceService;
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
        YearMonth previous = YearMonth.now().minusMonths(1);
        logger.info("API getPreviousMonthBalance called for userId={} for {}-{}", userId, previous.getYear(), previous.getMonthValue());
        var mbOpt = monthlyBalanceService.findByUserIdYearMonth(userId, previous.getYear(), previous.getMonthValue());
        return mbOpt.map(mb -> ResponseEntity.<Object>ok(mb))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("No monthly balance found for user for previous month"));
    }
}
