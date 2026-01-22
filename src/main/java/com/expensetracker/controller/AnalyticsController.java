package com.expensetracker.controller;

import com.expensetracker.dto.AnalyticsSummary;
import com.expensetracker.dto.ExpenseResponse;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.model.Income;
import com.expensetracker.service.AnalyticsService;
import com.expensetracker.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Controller for analytics endpoints without pagination.
 * Designed to provide full data for frontend analytics dashboards.
 *
 * Note: These endpoints return all records (up to MAX_ANALYTICS_RECORDS limit)
 * for comprehensive analytics without pagination constraints.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);
    private static final int MAX_DATE_RANGE_YEARS = 5; // Maximum 5 years of data

    private final AnalyticsService analyticsService;

    @Autowired
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // ==================== EXPENSE ENDPOINTS ====================

    /**
     * Get all expenses for a date range (no pagination).
     * POST /api/analytics/expenses/range
     * Body: { "userId": "...", "start": "YYYY-MM-DD", "end": "YYYY-MM-DD" }
     */
    @PostMapping("/expenses/range")
    public ResponseEntity<?> getExpensesByRange(@RequestBody Map<String, String> body) {
        logger.debug("Analytics: getExpensesByRange called with body: {}", body);

        String userId = body.get("userId");
        String startStr = body.get("start");
        String endStr = body.get("end");

        validateRangeRequest(userId, startStr, endStr);

        LocalDate start = LocalDate.parse(startStr);
        LocalDate end = LocalDate.parse(endStr);

        validateDateRange(start, end);

        List<ExpenseResponse> expenses = analyticsService.getAllExpensesForRange(userId, start, end);
        logger.info("Analytics: Retrieved {} expenses for userId: {} in range {} to {}", expenses.size(), userId, start, end);

        return ResponseEntity.ok(Map.of(
                "data", expenses,
                "totalRecords", expenses.size(),
                "maxRecordsLimit", Constants.MAX_ANALYTICS_RECORDS
        ));
    }

    /**
     * Get all expenses for a specific month (no pagination).
     * POST /api/analytics/expenses/month
     * Body: { "userId": "...", "year": 2024, "month": 1 }
     */
    @PostMapping("/expenses/month")
    public ResponseEntity<?> getExpensesForMonth(@RequestBody Map<String, Object> body) {
        logger.debug("Analytics: getExpensesForMonth called with body: {}", body);

        String userId = (String) body.get("userId");
        Integer year = getIntValue(body.get("year"));
        Integer month = getIntValue(body.get("month"));

        validateMonthRequest(userId, year, month);

        List<ExpenseResponse> expenses = analyticsService.getAllExpensesForMonth(userId, year, month);
        logger.info("Analytics: Retrieved {} expenses for userId: {} for {}/{}", expenses.size(), userId, year, month);

        return ResponseEntity.ok(Map.of(
                "data", expenses,
                "totalRecords", expenses.size(),
                "year", year,
                "month", month
        ));
    }

    /**
     * Get all expenses for a specific year (no pagination).
     * POST /api/analytics/expenses/year
     * Body: { "userId": "...", "year": 2024 }
     */
    @PostMapping("/expenses/year")
    public ResponseEntity<?> getExpensesForYear(@RequestBody Map<String, Object> body) {
        logger.debug("Analytics: getExpensesForYear called with body: {}", body);

        String userId = (String) body.get("userId");
        Integer year = getIntValue(body.get("year"));

        validateYearRequest(userId, year);

        List<ExpenseResponse> expenses = analyticsService.getAllExpensesForYear(userId, year);
        logger.info("Analytics: Retrieved {} expenses for userId: {} for year {}", expenses.size(), userId, year);

        return ResponseEntity.ok(Map.of(
                "data", expenses,
                "totalRecords", expenses.size(),
                "year", year
        ));
    }

    // ==================== INCOME ENDPOINTS ====================

    /**
     * Get all incomes for a date range (no pagination).
     * POST /api/analytics/incomes/range
     * Body: { "userId": "...", "start": "YYYY-MM-DD", "end": "YYYY-MM-DD" }
     */
    @PostMapping("/incomes/range")
    public ResponseEntity<?> getIncomesByRange(@RequestBody Map<String, String> body) {
        logger.debug("Analytics: getIncomesByRange called with body: {}", body);

        String userId = body.get("userId");
        String startStr = body.get("start");
        String endStr = body.get("end");

        validateRangeRequest(userId, startStr, endStr);

        LocalDate start = LocalDate.parse(startStr);
        LocalDate end = LocalDate.parse(endStr);

        validateDateRange(start, end);

        List<Income> incomes = analyticsService.getAllIncomesForRange(userId, start, end);
        logger.info("Analytics: Retrieved {} incomes for userId: {} in range {} to {}", incomes.size(), userId, start, end);

        return ResponseEntity.ok(Map.of(
                "data", incomes,
                "totalRecords", incomes.size(),
                "maxRecordsLimit", Constants.MAX_ANALYTICS_RECORDS
        ));
    }

    /**
     * Get all incomes for a specific month (no pagination).
     * POST /api/analytics/incomes/month
     * Body: { "userId": "...", "year": 2024, "month": 1 }
     */
    @PostMapping("/incomes/month")
    public ResponseEntity<?> getIncomesForMonth(@RequestBody Map<String, Object> body) {
        logger.debug("Analytics: getIncomesForMonth called with body: {}", body);

        String userId = (String) body.get("userId");
        Integer year = getIntValue(body.get("year"));
        Integer month = getIntValue(body.get("month"));

        validateMonthRequest(userId, year, month);

        List<Income> incomes = analyticsService.getAllIncomesForMonth(userId, year, month);
        logger.info("Analytics: Retrieved {} incomes for userId: {} for {}/{}", incomes.size(), userId, year, month);

        return ResponseEntity.ok(Map.of(
                "data", incomes,
                "totalRecords", incomes.size(),
                "year", year,
                "month", month
        ));
    }

    /**
     * Get all incomes for a specific year (no pagination).
     * POST /api/analytics/incomes/year
     * Body: { "userId": "...", "year": 2024 }
     */
    @PostMapping("/incomes/year")
    public ResponseEntity<?> getIncomesForYear(@RequestBody Map<String, Object> body) {
        logger.debug("Analytics: getIncomesForYear called with body: {}", body);

        String userId = (String) body.get("userId");
        Integer year = getIntValue(body.get("year"));

        validateYearRequest(userId, year);

        List<Income> incomes = analyticsService.getAllIncomesForYear(userId, year);
        logger.info("Analytics: Retrieved {} incomes for userId: {} for year {}", incomes.size(), userId, year);

        return ResponseEntity.ok(Map.of(
                "data", incomes,
                "totalRecords", incomes.size(),
                "year", year
        ));
    }

    // ==================== SUMMARY ENDPOINTS ====================

    /**
     * Get aggregated analytics summary for a date range.
     * POST /api/analytics/summary/range
     * Body: { "userId": "...", "start": "YYYY-MM-DD", "end": "YYYY-MM-DD" }
     */
    @PostMapping("/summary/range")
    public ResponseEntity<?> getSummaryForRange(@RequestBody Map<String, String> body) {
        logger.debug("Analytics: getSummaryForRange called with body: {}", body);

        String userId = body.get("userId");
        String startStr = body.get("start");
        String endStr = body.get("end");

        validateRangeRequest(userId, startStr, endStr);

        LocalDate start = LocalDate.parse(startStr);
        LocalDate end = LocalDate.parse(endStr);

        validateDateRange(start, end);

        AnalyticsSummary summary = analyticsService.getAnalyticsSummary(userId, start, end);
        logger.info("Analytics: Generated summary for userId: {} in range {} to {}", userId, start, end);

        return ResponseEntity.ok(summary);
    }

    /**
     * Get aggregated analytics summary for a specific month.
     * POST /api/analytics/summary/month
     * Body: { "userId": "...", "year": 2024, "month": 1 }
     */
    @PostMapping("/summary/month")
    public ResponseEntity<?> getSummaryForMonth(@RequestBody Map<String, Object> body) {
        logger.debug("Analytics: getSummaryForMonth called with body: {}", body);

        String userId = (String) body.get("userId");
        Integer year = getIntValue(body.get("year"));
        Integer month = getIntValue(body.get("month"));

        validateMonthRequest(userId, year, month);

        AnalyticsSummary summary = analyticsService.getAnalyticsSummaryForMonth(userId, year, month);
        logger.info("Analytics: Generated summary for userId: {} for {}/{}", userId, year, month);

        return ResponseEntity.ok(summary);
    }

    /**
     * Get aggregated analytics summary for a specific year.
     * POST /api/analytics/summary/year
     * Body: { "userId": "...", "year": 2024 }
     */
    @PostMapping("/summary/year")
    public ResponseEntity<?> getSummaryForYear(@RequestBody Map<String, Object> body) {
        logger.debug("Analytics: getSummaryForYear called with body: {}", body);

        String userId = (String) body.get("userId");
        Integer year = getIntValue(body.get("year"));

        validateYearRequest(userId, year);

        AnalyticsSummary summary = analyticsService.getAnalyticsSummaryForYear(userId, year);
        logger.info("Analytics: Generated summary for userId: {} for year {}", userId, year);

        return ResponseEntity.ok(summary);
    }

    // ==================== VALIDATION HELPERS ====================

    private void validateRangeRequest(String userId, String startStr, String endStr) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("userId is required");
        }
        if (startStr == null || startStr.isBlank()) {
            throw new BadRequestException("start date is required (format: YYYY-MM-DD)");
        }
        if (endStr == null || endStr.isBlank()) {
            throw new BadRequestException("end date is required (format: YYYY-MM-DD)");
        }
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new BadRequestException("start date must be before or equal to end date");
        }
        long years = ChronoUnit.YEARS.between(start, end);
        if (years > MAX_DATE_RANGE_YEARS) {
            throw new BadRequestException("Date range cannot exceed " + MAX_DATE_RANGE_YEARS + " years");
        }
    }

    private void validateMonthRequest(String userId, Integer year, Integer month) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("userId is required");
        }
        if (year == null) {
            throw new BadRequestException("year is required");
        }
        if (month == null) {
            throw new BadRequestException("month is required");
        }
        if (month < 1 || month > 12) {
            throw new BadRequestException("month must be between 1 and 12");
        }
        if (year < 2000 || year > 2100) {
            throw new BadRequestException("year must be between 2000 and 2100");
        }
    }

    private void validateYearRequest(String userId, Integer year) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("userId is required");
        }
        if (year == null) {
            throw new BadRequestException("year is required");
        }
        if (year < 2000 || year > 2100) {
            throw new BadRequestException("year must be between 2000 and 2100");
        }
    }

    private Integer getIntValue(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
