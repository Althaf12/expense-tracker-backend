package com.expensetracker.service;

import com.expensetracker.model.User;
import com.expensetracker.model.UserPreferences;
import com.expensetracker.repository.ExpenseAdjustmentRepository;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.IncomeRepository;
import com.expensetracker.repository.MonthlyBalanceRepository;
import com.expensetracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Service to update current_closing_balance for all users based on their
 * income month preference (P or C). This runs monthly after the monthly
 * balance snapshot has been created.
 *
 * <p>Formula depends on user's income month preference:</p>
 * <ul>
 *   <li><b>P (Previous month):</b> prev_month_closing + prev_month_income - current_month_expenses</li>
 *   <li><b>C (Current month):</b> prev_month_closing + current_month_income - current_month_expenses</li>
 * </ul>
 */
@Service
public class CurrentBalanceUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(CurrentBalanceUpdateService.class);

    private final UserRepository userRepository;
    private final MonthlyBalanceRepository monthlyBalanceRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseAdjustmentRepository adjustmentRepository;
    private final UserPreferencesService userPreferencesService;

    @Autowired
    public CurrentBalanceUpdateService(UserRepository userRepository,
                                      MonthlyBalanceRepository monthlyBalanceRepository,
                                      IncomeRepository incomeRepository,
                                      ExpenseRepository expenseRepository,
                                      ExpenseAdjustmentRepository adjustmentRepository,
                                      UserPreferencesService userPreferencesService) {
        this.userRepository = userRepository;
        this.monthlyBalanceRepository = monthlyBalanceRepository;
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.userPreferencesService = userPreferencesService;
    }

    /**
     * Updates current_closing_balance for all users.
     * This is called by the monthly scheduler after the monthly balance snapshot.
     */
    @CacheEvict(cacheNames = "users", allEntries = true)
    @Transactional
    public void updateCurrentBalanceForAllUsers() {
        List<User> allUsers = userRepository.findAll();
        logger.info("Starting current balance update for {} users", allUsers.size());

        int successCount = 0;
        int errorCount = 0;

        for (User user : allUsers) {
            try {
                updateCurrentBalanceForUser(user.getUserId());
                successCount++;
            } catch (Exception e) {
                logger.error("Failed to update current balance for userId: {}", user.getUserId(), e);
                errorCount++;
            }
        }

        logger.info("Current balance update complete: {} successful, {} errors", successCount, errorCount);
    }

    /**
     * Updates current_closing_balance for a single user based on their
     * income month preference.
     */
    @Transactional
    public void updateCurrentBalanceForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            logger.warn("Invalid userId provided: {}", userId);
            return;
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("User not found: {}", userId);
            return;
        }

        // Get user's income month preference (default to P if not set)
        String incomeMonth = "P"; // default
        Optional<UserPreferences> prefsOpt = userPreferencesService.findByUserId(userId);
        if (prefsOpt.isPresent() && prefsOpt.get().getIncomeMonth() != null) {
            incomeMonth = prefsOpt.get().getIncomeMonth();
        }

        YearMonth now = YearMonth.now();
        YearMonth prevMonth = now.minusMonths(1);

        // 1. Previous month closing balance
        BigDecimal prevClosing = monthlyBalanceRepository
                .findByUserIdAndYearAndMonth(userId, prevMonth.getYear(), prevMonth.getMonthValue())
                .map(mb -> mb.getClosingBalance() != null ? mb.getClosingBalance() : BigDecimal.ZERO)
                .orElse(BigDecimal.ZERO);

        // 2. Income (based on preference)
        BigDecimal income;
        LocalDate incomeStart, incomeEnd;

        if ("C".equals(incomeMonth)) {
            // Current month income
            incomeStart = now.atDay(1);
            incomeEnd = now.atEndOfMonth();
        } else {
            // Previous month income (default)
            incomeStart = prevMonth.atDay(1);
            incomeEnd = prevMonth.atEndOfMonth();
        }

        income = incomeRepository
                .findByUserIdAndReceivedDateBetween(userId, incomeStart, incomeEnd)
                .stream()
                .map(i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Current month expenses (net of completed adjustments)
        LocalDate curStart = now.atDay(1);
        LocalDate curEnd = now.atEndOfMonth();

        BigDecimal curExpenses = expenseRepository
                .findByUserIdAndExpenseDateBetween(userId, curStart, curEnd)
                .stream()
                .map(e -> e.getExpenseAmount() != null ? e.getExpenseAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Subtract completed adjustments
        BigDecimal completedAdjustments = adjustmentRepository
                .getTotalCompletedAdjustmentsForUserInRange(userId, curStart, curEnd);
        if (completedAdjustments == null) completedAdjustments = BigDecimal.ZERO;
        curExpenses = curExpenses.subtract(completedAdjustments).max(BigDecimal.ZERO);

        // Calculate new balance
        BigDecimal newBalance = prevClosing.add(income).subtract(curExpenses);

        // Update user
        User user = userOpt.get();
        user.setCurrentClosingBalance(newBalance);
        userRepository.save(user);

        logger.info("Updated current_closing_balance for userId={} (incomeMonth={}): prevClosing={} + income={} - expenses(net)={} = {}",
                userId, incomeMonth, prevClosing, income, curExpenses, newBalance);
    }
}

