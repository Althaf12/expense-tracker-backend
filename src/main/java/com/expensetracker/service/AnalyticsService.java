package com.expensetracker.service;

import com.expensetracker.dto.AnalyticsSummary;
import com.expensetracker.dto.ExpenseResponse;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.model.UserExpenseCategory;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.IncomeRepository;
import com.expensetracker.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analytics data retrieval without pagination limits.
 * Designed to support frontend analytics dashboards that need full data.
 */
@Service
public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final UserExpenseCategoryService userExpenseCategoryService;

    @Autowired
    public AnalyticsService(ExpenseRepository expenseRepository,
                            IncomeRepository incomeRepository,
                            UserExpenseCategoryService userExpenseCategoryService) {
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.userExpenseCategoryService = userExpenseCategoryService;
    }

    /**
     * Get all expenses for a user within a date range (no pagination).
     * Limited to MAX_ANALYTICS_RECORDS to prevent memory issues.
     */
    public List<ExpenseResponse> getAllExpensesForRange(String userId, LocalDate start, LocalDate end) {
        logger.debug("Fetching all expenses for userId: {} from {} to {}", userId, start, end);
        List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end);

        if (expenses.size() > Constants.MAX_ANALYTICS_RECORDS) {
            logger.warn("Expense count {} exceeds max limit {}, truncating", expenses.size(), Constants.MAX_ANALYTICS_RECORDS);
            expenses = expenses.subList(0, Constants.MAX_ANALYTICS_RECORDS);
        }

        return mapExpensesToResponses(expenses);
    }

    /**
     * Get all expenses for a user for a specific month (no pagination).
     */
    public List<ExpenseResponse> getAllExpensesForMonth(String userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return getAllExpensesForRange(userId, start, end);
    }

    /**
     * Get all expenses for a user for a specific year (no pagination).
     */
    public List<ExpenseResponse> getAllExpensesForYear(String userId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return getAllExpensesForRange(userId, start, end);
    }

    /**
     * Get all incomes for a user within a date range (no pagination).
     * Limited to MAX_ANALYTICS_RECORDS to prevent memory issues.
     */
    public List<Income> getAllIncomesForRange(String userId, LocalDate start, LocalDate end) {
        logger.debug("Fetching all incomes for userId: {} from {} to {}", userId, start, end);
        List<Income> incomes = incomeRepository.findByUserIdAndReceivedDateBetween(userId, start, end);

        if (incomes.size() > Constants.MAX_ANALYTICS_RECORDS) {
            logger.warn("Income count {} exceeds max limit {}, truncating", incomes.size(), Constants.MAX_ANALYTICS_RECORDS);
            incomes = incomes.subList(0, Constants.MAX_ANALYTICS_RECORDS);
        }

        return incomes;
    }

    /**
     * Get all incomes for a user for a specific month (no pagination).
     */
    public List<Income> getAllIncomesForMonth(String userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return getAllIncomesForRange(userId, start, end);
    }

    /**
     * Get all incomes for a user for a specific year (no pagination).
     */
    public List<Income> getAllIncomesForYear(String userId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return getAllIncomesForRange(userId, start, end);
    }

    /**
     * Get aggregated analytics summary for a user within a date range.
     */
    public AnalyticsSummary getAnalyticsSummary(String userId, LocalDate start, LocalDate end) {
        logger.info("Generating analytics summary for userId: {} from {} to {}", userId, start, end);

        List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end);
        List<Income> incomes = incomeRepository.findByUserIdAndReceivedDateBetween(userId, start, end);

        // Total calculations
        double totalExpenses = expenses.stream()
                .mapToDouble(e -> e.getExpenseAmount() != null ? e.getExpenseAmount() : 0.0)
                .sum();
        double totalIncome = incomes.stream()
                .mapToDouble(i -> i.getAmount() != null ? i.getAmount() : 0.0)
                .sum();

        // Expenses by category
        Map<String, Double> expensesByCategory = new LinkedHashMap<>();
        Map<Integer, String> categoryCache = new HashMap<>();
        for (Expense e : expenses) {
            Integer catId = e.getUserExpenseCategoryId();
            String catName = "Uncategorized";
            if (catId != null) {
                catName = categoryCache.computeIfAbsent(catId, id -> {
                    Optional<UserExpenseCategory> cat = userExpenseCategoryService.findById(id);
                    return cat.map(UserExpenseCategory::getUserExpenseCategoryName).orElse("Unknown");
                });
            }
            double amount = e.getExpenseAmount() != null ? e.getExpenseAmount() : 0.0;
            expensesByCategory.merge(catName, amount, Double::sum);
        }

        // Incomes by source
        Map<String, Double> incomesBySource = incomes.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getSource() != null ? i.getSource() : "Unknown",
                        LinkedHashMap::new,
                        Collectors.summingDouble(i -> i.getAmount() != null ? i.getAmount() : 0.0)
                ));

        // Monthly expense trend
        Map<String, Double> monthlyExpenseTrend = expenses.stream()
                .filter(e -> e.getExpenseDate() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getExpenseDate().format(MONTH_FORMATTER),
                        TreeMap::new,
                        Collectors.summingDouble(e -> e.getExpenseAmount() != null ? e.getExpenseAmount() : 0.0)
                ));

        // Monthly income trend
        Map<String, Double> monthlyIncomeTrend = incomes.stream()
                .filter(i -> i.getReceivedDate() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getReceivedDate().format(MONTH_FORMATTER),
                        TreeMap::new,
                        Collectors.summingDouble(i -> i.getAmount() != null ? i.getAmount() : 0.0)
                ));

        return AnalyticsSummary.builder()
                .totalExpenses(totalExpenses)
                .totalIncome(totalIncome)
                .netBalance(totalIncome - totalExpenses)
                .totalExpenseCount(expenses.size())
                .totalIncomeCount(incomes.size())
                .expensesByCategory(expensesByCategory)
                .incomesBySource(incomesBySource)
                .monthlyExpenseTrend(monthlyExpenseTrend)
                .monthlyIncomeTrend(monthlyIncomeTrend)
                .build();
    }

    /**
     * Get analytics summary for a specific year.
     */
    public AnalyticsSummary getAnalyticsSummaryForYear(String userId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return getAnalyticsSummary(userId, start, end);
    }

    /**
     * Get analytics summary for a specific month.
     */
    public AnalyticsSummary getAnalyticsSummaryForMonth(String userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return getAnalyticsSummary(userId, start, end);
    }

    private List<ExpenseResponse> mapExpensesToResponses(List<Expense> expenses) {
        List<ExpenseResponse> responses = new ArrayList<>();
        Map<Integer, String> categoryCache = new HashMap<>();

        for (Expense e : expenses) {
            ExpenseResponse r = new ExpenseResponse();
            r.setExpensesId(e.getExpensesId());
            r.setUserId(e.getUserId());
            r.setExpenseName(e.getExpenseName());
            r.setExpenseAmount(e.getExpenseAmount());
            r.setLastUpdateTmstp(e.getLastUpdateTmstp());
            r.setExpenseDate(e.getExpenseDate());

            // Resolve category name with caching
            String catName = null;
            if (e.getUserExpenseCategoryId() != null) {
                catName = categoryCache.computeIfAbsent(e.getUserExpenseCategoryId(), id -> {
                    Optional<UserExpenseCategory> catOpt = userExpenseCategoryService.findById(id);
                    return catOpt.map(UserExpenseCategory::getUserExpenseCategoryName).orElse(null);
                });
            }
            r.setUserExpenseCategoryName(catName);
            responses.add(r);
        }
        return responses;
    }
}
