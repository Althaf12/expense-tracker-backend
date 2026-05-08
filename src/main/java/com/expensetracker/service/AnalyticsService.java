package com.expensetracker.service;

import com.expensetracker.dto.AnalyticsSummary;
import com.expensetracker.dto.CategoryAnalyticsSummary;
import com.expensetracker.dto.ExpenseResponse;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseAdjustment;
import com.expensetracker.model.Income;
import com.expensetracker.model.UserExpenseCategory;
import com.expensetracker.model.UserPreferences;
import com.expensetracker.repository.ExpenseAdjustmentRepository;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.IncomeRepository;
import com.expensetracker.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analytics data retrieval without pagination limits.
 * Designed to support frontend analytics dashboards that need full data.
 *
 * <p>Income date ranges respect the user's {@code incomeMonth} preference:
 * <ul>
 *   <li>C (Current): income is fetched for the same period as expenses.</li>
 *   <li>P (Previous): income is fetched from one month earlier (e.g., for expense
 *       month May, income is fetched for April).</li>
 * </ul>
 */
@Service
public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final UserExpenseCategoryService userExpenseCategoryService;
    private final ExpenseAdjustmentRepository adjustmentRepository;
    private final UserPreferencesService userPreferencesService;

    @Autowired
    public AnalyticsService(ExpenseRepository expenseRepository,
                            IncomeRepository incomeRepository,
                            UserExpenseCategoryService userExpenseCategoryService,
                            ExpenseAdjustmentRepository adjustmentRepository,
                            UserPreferencesService userPreferencesService) {
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.userExpenseCategoryService = userExpenseCategoryService;
        this.adjustmentRepository = adjustmentRepository;
        this.userPreferencesService = userPreferencesService;
    }

    // ==================== INNER RESULT WRAPPER ====================

    /**
     * Wraps income query results together with the actual income date range used
     * and the preference that drove it.
     */
    public static class IncomeResult {
        public final List<Income> incomes;
        public final LocalDate incomeStart;
        public final LocalDate incomeEnd;
        public final String preference;

        public IncomeResult(List<Income> incomes, LocalDate incomeStart,
                            LocalDate incomeEnd, String preference) {
            this.incomes = incomes;
            this.incomeStart = incomeStart;
            this.incomeEnd = incomeEnd;
            this.preference = preference;
        }
    }

    // ==================== EXPENSE ENDPOINTS ====================

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

    // ==================== INCOME ENDPOINTS (PREFERENCE-AWARE) ====================

    /**
     * Get all incomes for a user within a date range, respecting the income month preference.
     * If preference is P, the income start date is shifted one month earlier.
     */
    public IncomeResult getAllIncomesForRange(String userId, LocalDate start, LocalDate end) {
        logger.debug("Fetching all incomes for userId: {} (range {}-{}), resolving preference", userId, start, end);
        LocalDate[] incomeRange = resolveIncomeRangeForRange(userId, start, end);
        String pref = getIncomePreference(userId);

        List<Income> incomes = fetchIncomesRaw(userId, incomeRange[0], incomeRange[1]);
        logger.info("Fetched {} incomes for userId: {} using income range {} to {} (pref={})",
                incomes.size(), userId, incomeRange[0], incomeRange[1], pref);
        return new IncomeResult(incomes, incomeRange[0], incomeRange[1], pref);
    }

    /**
     * Get all incomes for a user for a specific month, respecting the income month preference.
     * If preference is P, the previous month's income is returned.
     */
    public IncomeResult getAllIncomesForMonth(String userId, int year, int month) {
        LocalDate[] incomeRange = resolveIncomeRangeForMonth(userId, year, month);
        String pref = getIncomePreference(userId);

        List<Income> incomes = fetchIncomesRaw(userId, incomeRange[0], incomeRange[1]);
        logger.info("Fetched {} incomes for userId: {}, requested month={}/{}, using income range {} to {} (pref={})",
                incomes.size(), userId, year, month, incomeRange[0], incomeRange[1], pref);
        return new IncomeResult(incomes, incomeRange[0], incomeRange[1], pref);
    }

    /**
     * Get all incomes for a user for a specific year, respecting the income month preference.
     * If preference is P, income is fetched from December of (year-1) to
     * the lesser of November of (year) and the end of the previous month.
     */
    public IncomeResult getAllIncomesForYear(String userId, int year) {
        LocalDate[] incomeRange = resolveIncomeRangeForYear(userId, year);
        String pref = getIncomePreference(userId);

        List<Income> incomes = fetchIncomesRaw(userId, incomeRange[0], incomeRange[1]);
        logger.info("Fetched {} incomes for userId: {}, requested year={}, using income range {} to {} (pref={})",
                incomes.size(), userId, year, incomeRange[0], incomeRange[1], pref);
        return new IncomeResult(incomes, incomeRange[0], incomeRange[1], pref);
    }

    // ==================== SUMMARY ENDPOINTS ====================

    /**
     * Get aggregated analytics summary for a user within a date range.
     * Income is fetched according to the user's income month preference.
     */
    public AnalyticsSummary getAnalyticsSummary(String userId, LocalDate expenseStart, LocalDate expenseEnd) {
        logger.info("Generating analytics summary for userId: {} from {} to {}", userId, expenseStart, expenseEnd);
        LocalDate[] incomeRange = resolveIncomeRangeForRange(userId, expenseStart, expenseEnd);
        String pref = getIncomePreference(userId);
        return buildAnalyticsSummary(userId, expenseStart, expenseEnd, incomeRange[0], incomeRange[1], pref);
    }

    /**
     * Get analytics summary for a specific year.
     * Income is fetched according to the user's income month preference.
     */
    public AnalyticsSummary getAnalyticsSummaryForYear(String userId, int year) {
        LocalDate expenseStart = LocalDate.of(year, 1, 1);
        LocalDate expenseEnd = LocalDate.of(year, 12, 31);
        LocalDate[] incomeRange = resolveIncomeRangeForYear(userId, year);
        String pref = getIncomePreference(userId);
        return buildAnalyticsSummary(userId, expenseStart, expenseEnd, incomeRange[0], incomeRange[1], pref);
    }

    /**
     * Get analytics summary for a specific month.
     * Income is fetched according to the user's income month preference.
     */
    public AnalyticsSummary getAnalyticsSummaryForMonth(String userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate expenseStart = ym.atDay(1);
        LocalDate expenseEnd = ym.atEndOfMonth();
        LocalDate[] incomeRange = resolveIncomeRangeForMonth(userId, year, month);
        String pref = getIncomePreference(userId);
        return buildAnalyticsSummary(userId, expenseStart, expenseEnd, incomeRange[0], incomeRange[1], pref);
    }

    // ==================== CATEGORY SUMMARY ENDPOINTS ====================

    /**
     * Get consolidated expense totals per category for a date range.
     */
    public CategoryAnalyticsSummary getCategoryExpenseSummaryForRange(String userId, LocalDate start, LocalDate end) {
        logger.info("Generating category expense summary for userId: {} from {} to {}", userId, start, end);
        List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end);
        return buildCategoryExpenseSummary(expenses);
    }

    /**
     * Get consolidated expense totals per category for a specific month.
     */
    public CategoryAnalyticsSummary getCategoryExpenseSummaryForMonth(String userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return getCategoryExpenseSummaryForRange(userId, ym.atDay(1), ym.atEndOfMonth());
    }

    /**
     * Get consolidated expense totals per category for a specific year.
     */
    public CategoryAnalyticsSummary getCategoryExpenseSummaryForYear(String userId, int year) {
        return getCategoryExpenseSummaryForRange(userId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * Build the full AnalyticsSummary using separate expense and income date windows.
     */
    private AnalyticsSummary buildAnalyticsSummary(String userId,
                                                    LocalDate expenseStart, LocalDate expenseEnd,
                                                    LocalDate incomeStart, LocalDate incomeEnd,
                                                    String incomePreference) {
        List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, expenseStart, expenseEnd);
        List<Income> incomes = fetchIncomesRaw(userId, incomeStart, incomeEnd);

        // Collect expense IDs for adjustment calculation
        List<Integer> expenseIds = expenses.stream()
                .map(Expense::getExpensesId)
                .collect(Collectors.toList());

        // Get adjustment map
        Map<Integer, BigDecimal> adjustmentsMap = getCompletedAdjustmentsMap(expenseIds);

        // Get total adjustments for the date range
        BigDecimal totalAdjustments = adjustmentRepository.getTotalCompletedAdjustmentsForUserInRange(userId, expenseStart, expenseEnd);
        if (totalAdjustments == null) {
            totalAdjustments = BigDecimal.ZERO;
        }

        // Total calculations using BigDecimal
        BigDecimal totalExpenses = expenses.stream()
                .map(e -> e.getExpenseAmount() != null ? e.getExpenseAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIncome = incomes.stream()
                .map(i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Net expenses after adjustments
        BigDecimal netExpenses = totalExpenses.subtract(totalAdjustments).max(BigDecimal.ZERO);

        // Expenses by category
        Map<String, BigDecimal> expensesByCategory = new LinkedHashMap<>();
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
            BigDecimal amount = e.getExpenseAmount() != null ? e.getExpenseAmount() : BigDecimal.ZERO;
            // Subtract completed adjustments for this expense
            BigDecimal adjAmount = adjustmentsMap.getOrDefault(e.getExpensesId(), BigDecimal.ZERO);
            BigDecimal netAmount = amount.subtract(adjAmount).max(BigDecimal.ZERO);
            expensesByCategory.merge(catName, netAmount, BigDecimal::add);
        }

        // Incomes by source
        Map<String, BigDecimal> incomesBySource = incomes.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getSource() != null ? i.getSource() : "Unknown",
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO,
                                BigDecimal::add)
                ));

        // Monthly expense trend (net after adjustments)
        Map<String, BigDecimal> monthlyExpenseTrend = new TreeMap<>();
        for (Expense e : expenses) {
            if (e.getExpenseDate() == null) continue;
            String monthKey = e.getExpenseDate().format(MONTH_FORMATTER);
            BigDecimal amount = e.getExpenseAmount() != null ? e.getExpenseAmount() : BigDecimal.ZERO;
            BigDecimal adjAmount = adjustmentsMap.getOrDefault(e.getExpensesId(), BigDecimal.ZERO);
            BigDecimal netAmount = amount.subtract(adjAmount).max(BigDecimal.ZERO);
            monthlyExpenseTrend.merge(monthKey, netAmount, BigDecimal::add);
        }

        // Monthly income trend
        Map<String, BigDecimal> monthlyIncomeTrend = incomes.stream()
                .filter(i -> i.getReceivedDate() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getReceivedDate().format(MONTH_FORMATTER),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO,
                                BigDecimal::add)
                ));

        return AnalyticsSummary.builder()
                .totalExpenses(totalExpenses)
                .totalIncome(totalIncome)
                .netBalance(totalIncome.subtract(netExpenses))
                .totalAdjustments(totalAdjustments)
                .netExpenses(netExpenses)
                .totalExpenseCount(expenses.size())
                .totalIncomeCount(incomes.size())
                .expensesByCategory(expensesByCategory)
                .incomesBySource(incomesBySource)
                .monthlyExpenseTrend(monthlyExpenseTrend)
                .monthlyIncomeTrend(monthlyIncomeTrend)
                .incomeRangeStart(incomeStart)
                .incomeRangeEnd(incomeEnd)
                .incomeMonthPreference(incomePreference)
                .build();
    }

    /**
     * Build a CategoryAnalyticsSummary from a list of expenses.
     */
    private CategoryAnalyticsSummary buildCategoryExpenseSummary(List<Expense> expenses) {
        if (expenses.isEmpty()) {
            return CategoryAnalyticsSummary.builder()
                    .totalExpenses(BigDecimal.ZERO)
                    .totalAdjustments(BigDecimal.ZERO)
                    .netExpenses(BigDecimal.ZERO)
                    .totalRecords(0)
                    .categoryTotals(Collections.emptyMap())
                    .categoryGrossTotals(Collections.emptyMap())
                    .categoryAdjustments(Collections.emptyMap())
                    .categoryRecordCounts(Collections.emptyMap())
                    .build();
        }

        List<Integer> expenseIds = expenses.stream()
                .map(Expense::getExpensesId)
                .collect(Collectors.toList());
        Map<Integer, BigDecimal> adjustmentsMap = getCompletedAdjustmentsMap(expenseIds);

        Map<Integer, String> categoryCache = new HashMap<>();
        Map<String, BigDecimal> categoryGross = new LinkedHashMap<>();
        Map<String, BigDecimal> categoryAdj = new LinkedHashMap<>();
        Map<String, BigDecimal> categoryNet = new LinkedHashMap<>();
        Map<String, Integer> categoryCounts = new LinkedHashMap<>();

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalAdj = BigDecimal.ZERO;

        for (Expense e : expenses) {
            Integer catId = e.getUserExpenseCategoryId();
            String catName = "Uncategorized";
            if (catId != null) {
                catName = categoryCache.computeIfAbsent(catId, id -> {
                    Optional<UserExpenseCategory> cat = userExpenseCategoryService.findById(id);
                    return cat.map(UserExpenseCategory::getUserExpenseCategoryName).orElse("Unknown");
                });
            }

            BigDecimal gross = e.getExpenseAmount() != null ? e.getExpenseAmount() : BigDecimal.ZERO;
            BigDecimal adj = adjustmentsMap.getOrDefault(e.getExpensesId(), BigDecimal.ZERO);
            BigDecimal net = gross.subtract(adj).max(BigDecimal.ZERO);

            categoryGross.merge(catName, gross, BigDecimal::add);
            categoryAdj.merge(catName, adj, BigDecimal::add);
            categoryNet.merge(catName, net, BigDecimal::add);
            categoryCounts.merge(catName, 1, Integer::sum);

            totalGross = totalGross.add(gross);
            totalAdj = totalAdj.add(adj);
        }

        BigDecimal totalNet = totalGross.subtract(totalAdj).max(BigDecimal.ZERO);

        return CategoryAnalyticsSummary.builder()
                .totalExpenses(totalGross)
                .totalAdjustments(totalAdj)
                .netExpenses(totalNet)
                .totalRecords(expenses.size())
                .categoryTotals(categoryNet)
                .categoryGrossTotals(categoryGross)
                .categoryAdjustments(categoryAdj)
                .categoryRecordCounts(categoryCounts)
                .build();
    }

    /**
     * Raw income fetch without preference resolution.
     */
    private List<Income> fetchIncomesRaw(String userId, LocalDate start, LocalDate end) {
        List<Income> incomes = incomeRepository.findByUserIdAndReceivedDateBetween(userId, start, end);
        if (incomes.size() > Constants.MAX_ANALYTICS_RECORDS) {
            logger.warn("Income count {} exceeds max limit {}, truncating", incomes.size(), Constants.MAX_ANALYTICS_RECORDS);
            incomes = incomes.subList(0, Constants.MAX_ANALYTICS_RECORDS);
        }
        return incomes;
    }

    /**
     * Retrieve the user's incomeMonth preference. Returns "C" if not set or not found.
     */
    public String getIncomePreference(String userId) {
        try {
            Optional<UserPreferences> prefsOpt = userPreferencesService.findByUserId(userId);
            if (prefsOpt.isPresent()) {
                String pref = prefsOpt.get().getIncomeMonth();
                if (pref != null && !pref.isBlank()) {
                    return pref.trim().toUpperCase();
                }
            }
        } catch (Exception ex) {
            logger.warn("Failed to read user preferences for userId {}. Defaulting to C.", userId, ex);
        }
        return Constants.DEFAULT_INCOME_MONTH;
    }

    /**
     * Resolve the income date range for a range-based query.
     * <ul>
     *   <li>C: income range = [start, end] (unchanged)</li>
     *   <li>P: income range = [first day of month before start, end]</li>
     * </ul>
     */
    public LocalDate[] resolveIncomeRangeForRange(String userId, LocalDate start, LocalDate end) {
        if ("P".equalsIgnoreCase(getIncomePreference(userId))) {
            LocalDate adjustedStart = start.withDayOfMonth(1).minusMonths(1);
            return new LocalDate[]{adjustedStart, end};
        }
        return new LocalDate[]{start, end};
    }

    /**
     * Resolve the income date range for a month-based query.
     * <ul>
     *   <li>C: income = requested month</li>
     *   <li>P: income = previous month</li>
     * </ul>
     */
    public LocalDate[] resolveIncomeRangeForMonth(String userId, int year, int month) {
        YearMonth ym;
        if ("P".equalsIgnoreCase(getIncomePreference(userId))) {
            ym = YearMonth.of(year, month).minusMonths(1);
        } else {
            ym = YearMonth.of(year, month);
        }
        return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
    }

    /**
     * Resolve the income date range for a year-based query.
     * <ul>
     *   <li>C: income = Jan 1 to Dec 31 of the requested year</li>
     *   <li>P: income = Dec 1 of (year-1) to min(Nov 30 of year, end of previous month from today)</li>
     * </ul>
     */
    public LocalDate[] resolveIncomeRangeForYear(String userId, int year) {
        if ("P".equalsIgnoreCase(getIncomePreference(userId))) {
            LocalDate incomeStart = LocalDate.of(year - 1, 12, 1);
            // Cap at end of the previous month from today so we don't include future months
            LocalDate endOfPrevMonth = LocalDate.now().withDayOfMonth(1).minusDays(1);
            LocalDate maxEnd = LocalDate.of(year, 11, 30);
            LocalDate incomeEnd = maxEnd.isBefore(endOfPrevMonth) ? maxEnd : endOfPrevMonth;
            return new LocalDate[]{incomeStart, incomeEnd};
        }
        return new LocalDate[]{LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)};
    }

    // ==================== EXPENSE RESPONSE MAPPING ====================

    private List<ExpenseResponse> mapExpensesToResponses(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return new ArrayList<>();
        }

        // Collect expense IDs for batch fetching adjustments
        List<Integer> expenseIds = expenses.stream()
                .map(Expense::getExpensesId)
                .collect(Collectors.toList());

        // Batch fetch completed adjustments for all expenses
        Map<Integer, BigDecimal> adjustmentsMap = getCompletedAdjustmentsMap(expenseIds);

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

            // Calculate net expense amount after adjustments
            BigDecimal totalAdj = adjustmentsMap.getOrDefault(e.getExpensesId(), BigDecimal.ZERO);
            r.setTotalAdjustments(totalAdj);

            BigDecimal expAmt = e.getExpenseAmount() != null ? e.getExpenseAmount() : BigDecimal.ZERO;
            BigDecimal netAmount = expAmt.subtract(totalAdj);
            r.setNetExpenseAmount(netAmount.max(BigDecimal.ZERO));

            responses.add(r);
        }
        return responses;
    }

    /**
     * Get a map of expense ID to total completed adjustment amount.
     */
    private Map<Integer, BigDecimal> getCompletedAdjustmentsMap(List<Integer> expenseIds) {
        if (expenseIds == null || expenseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ExpenseAdjustment> completedAdjustments = adjustmentRepository.findCompletedAdjustmentsForExpenses(expenseIds);
        return completedAdjustments.stream()
                .collect(Collectors.groupingBy(
                        ExpenseAdjustment::getExpensesId,
                        Collectors.reducing(BigDecimal.ZERO,
                                ExpenseAdjustment::getAdjustmentAmount,
                                BigDecimal::add)
                ));
    }
}
