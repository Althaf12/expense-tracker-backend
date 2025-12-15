package com.expensetracker.controller;

import com.expensetracker.service.MonthlyBalanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/monthly-balance")
public class MonthlyBalanceController {

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

        monthlyBalanceService.generateForAllUsersAndMonth(target);
        String msg = String.format("Monthly balances generated for %d-%02d", target.getYear(), target.getMonthValue());
        return ResponseEntity.ok(msg);
    }

    /**
     * Fetch the MonthlyBalance snapshot for the previous month for a given user.
     * Example: GET /api/monthly-balance/previous?username=john
     */
    @GetMapping("/previous")
    public ResponseEntity<?> getPreviousMonthBalance(@RequestParam String username) {
        YearMonth previous = YearMonth.now().minusMonths(1);
        var mbOpt = monthlyBalanceService.findByUsernameYearMonth(username, previous.getYear(), previous.getMonthValue());
        return mbOpt.map(mb -> ResponseEntity.<Object>ok(mb))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("No monthly balance found for user for previous month"));
    }
}
