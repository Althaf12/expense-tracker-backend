package com.expensetracker.service;

import com.expensetracker.model.User;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Maintains the denormalized {@code current_closing_balance} on the {@code users} table.
 *
 * <p>Formula (refreshed on every expense / income / monthly-balance mutation):</p>
 * <pre>
 *   current_closing_balance =
 *       previous_month.closing_balance          (from monthly_balance table)
 *     + total_income_of_previous_month          (from income table, received_date)
 *     - total_expenses_of_current_month         (from expense table, expense_date)
 * </pre>
 * "Current month" is always {@link YearMonth#now()}.
 */
@Service
public class ClosingBalanceService {

    private static final Logger logger = LoggerFactory.getLogger(ClosingBalanceService.class);

    private final UserRepository             userRepository;
    private final MonthlyBalanceRepository   monthlyBalanceRepository;
    private final IncomeRepository           incomeRepository;
    private final ExpenseRepository          expenseRepository;
    private final ExpenseAdjustmentRepository adjustmentRepository;

    @Autowired
    public ClosingBalanceService(UserRepository userRepository,
                                 MonthlyBalanceRepository monthlyBalanceRepository,
                                 IncomeRepository incomeRepository,
                                 ExpenseRepository expenseRepository,
                                 ExpenseAdjustmentRepository adjustmentRepository) {
        this.userRepository           = userRepository;
        this.monthlyBalanceRepository = monthlyBalanceRepository;
        this.incomeRepository         = incomeRepository;
        this.expenseRepository        = expenseRepository;
        this.adjustmentRepository     = adjustmentRepository;
    }

    /**
     * Recomputes and persists {@code current_closing_balance} for the given user.
     * Also evicts the "users" cache so stale data is never served.
     */
    @CacheEvict(cacheNames = "users", allEntries = true)
    public void recalculate(String userId) {
        if (userId == null || userId.isBlank()) return;

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("ClosingBalanceService.recalculate: user not found – {}", userId);
            return;
        }

        YearMonth now         = YearMonth.now();
        YearMonth prevMonth   = now.minusMonths(1);

        // 1. Previous month closing balance (from monthly_balance table)
        BigDecimal prevClosing = monthlyBalanceRepository
                .findByUserIdAndYearAndMonth(userId, prevMonth.getYear(), prevMonth.getMonthValue())
                .map(mb -> mb.getClosingBalance() != null ? mb.getClosingBalance() : BigDecimal.ZERO)
                .orElse(BigDecimal.ZERO);

        // 2. Total income of previous month
        LocalDate prevStart = prevMonth.atDay(1);
        LocalDate prevEnd   = prevMonth.atEndOfMonth();
        BigDecimal prevIncome = incomeRepository
                .findByUserIdAndReceivedDateBetween(userId, prevStart, prevEnd)
                .stream()
                .map(i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Total expenses of current month (net of completed adjustments — same logic as MonthlyBalanceService)
        LocalDate curStart = now.atDay(1);
        LocalDate curEnd   = now.atEndOfMonth();
        BigDecimal curExpenses = expenseRepository
                .findByUserIdAndExpenseDateBetween(userId, curStart, curEnd)
                .stream()
                .map(e -> e.getExpenseAmount() != null ? e.getExpenseAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Subtract completed adjustments (refunds / cashbacks / reversals) for current month
        BigDecimal completedAdjustments = adjustmentRepository
                .getTotalCompletedAdjustmentsForUserInRange(userId, curStart, curEnd);
        if (completedAdjustments == null) completedAdjustments = BigDecimal.ZERO;
        curExpenses = curExpenses.subtract(completedAdjustments).max(BigDecimal.ZERO);

        BigDecimal newBalance = prevClosing.add(prevIncome).subtract(curExpenses);

        User user = userOpt.get();
        user.setCurrentClosingBalance(newBalance);
        userRepository.save(user);

        logger.info("Recalculated current_closing_balance for userId={}: prevClosing={} + prevIncome={} - curExpenses(net)={} = {}",
                userId, prevClosing, prevIncome, curExpenses, newBalance);
    }
}





