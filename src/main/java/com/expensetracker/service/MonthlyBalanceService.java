package com.expensetracker.service;

import com.expensetracker.model.MonthlyBalance;
import com.expensetracker.model.Income;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.MonthlyBalanceRepository;
import com.expensetracker.repository.IncomeRepository;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.model.User;
import com.expensetracker.model.UserPreferences;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class MonthlyBalanceService {

    private static final Logger logger = LoggerFactory.getLogger(MonthlyBalanceService.class);

    private final MonthlyBalanceRepository monthlyBalanceRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final UserPreferencesService userPreferencesService;

    public MonthlyBalanceService(MonthlyBalanceRepository monthlyBalanceRepository,
                                 IncomeRepository incomeRepository,
                                 ExpenseRepository expenseRepository,
                                 UserRepository userRepository,
                                 UserPreferencesService userPreferencesService) {
        this.monthlyBalanceRepository = monthlyBalanceRepository;
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.userPreferencesService = userPreferencesService;
    }

    public Optional<MonthlyBalance> findLatestForUser(String userId) {
        return monthlyBalanceRepository.findTopByUserIdOrderByYearDescMonthDesc(userId);
    }

    public Optional<MonthlyBalance> findByUserIdYearMonth(String userId, int year, int month) {
        return monthlyBalanceRepository.findByUserIdAndYearAndMonth(userId, year, month);
    }

    private double totalIncomeForMonth(String userId, YearMonth ym) {
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<Income> incomes = incomeRepository.findByUserIdAndReceivedDateBetween(userId, start, end);
        return incomes.stream().mapToDouble(i -> i.getAmount() == null ? 0.0 : i.getAmount()).sum();
    }

    private double totalExpensesForMonth(String userId, YearMonth ym) {
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end);
        return expenses.stream().mapToDouble(e -> e.getExpenseAmount() == null ? 0.0 : e.getExpenseAmount()).sum();
    }

    private double previousMonthClosingBalance(String userId, YearMonth targetMonth) {
        // previous month
        YearMonth prev = targetMonth.minusMonths(1);
        Optional<MonthlyBalance> prevBalance = monthlyBalanceRepository.findByUserIdAndYearAndMonth(userId, prev.getYear(), prev.getMonthValue());
        return prevBalance.map(MonthlyBalance::getClosingBalance).orElse(0.0);
    }

    @Transactional
    public MonthlyBalance generateForUserAndMonth(String userId, YearMonth targetMonth) {
        // ensure idempotency
        Optional<MonthlyBalance> existing = monthlyBalanceRepository.findByUserIdAndYearAndMonth(userId, targetMonth.getYear(), targetMonth.getMonthValue());
        if (existing.isPresent()) {
            logger.info("Monthly balance already exists for userId {} for {}-{}", userId, targetMonth.getYear(), targetMonth.getMonthValue());
            return existing.get();
        }

        // Determine which month to use for incomes based on user's preference
        YearMonth incomesMonthToUse = targetMonth.minusMonths(1); // default previous month
        try {
            Optional<UserPreferences> prefsOpt = userPreferencesService.findByUserId(userId);
            if (prefsOpt.isPresent()) {
                UserPreferences prefs = prefsOpt.get();
                String incomeMonthPref = prefs.getIncomeMonth();
                if (incomeMonthPref != null && incomeMonthPref.equalsIgnoreCase("C")) {
                    incomesMonthToUse = targetMonth; // current month incomes
                }
            }
        } catch (Exception ex) {
            logger.warn("Failed to read user preferences for user {}. Falling back to previous month incomes.", userId, ex);
        }

        double opening = previousMonthClosingBalance(userId, targetMonth);
        double income = totalIncomeForMonth(userId, incomesMonthToUse);
        double expenses = totalExpensesForMonth(userId, targetMonth);

        double closing = opening + income - expenses;

        MonthlyBalance mb = new MonthlyBalance();
        mb.setUserId(userId);
        mb.setYear(targetMonth.getYear());
        mb.setMonth(targetMonth.getMonthValue());
        mb.setOpeningBalance(opening);
        mb.setClosingBalance(closing);

        logger.info("Generated monthly balance for userId {} for {}-{} (income used from {}-{}): opening={}, income={}, expenses={}, closing= {}",
                userId, targetMonth.getYear(), targetMonth.getMonthValue(), incomesMonthToUse.getYear(), incomesMonthToUse.getMonthValue(), opening, income, expenses, closing);
        return monthlyBalanceRepository.save(mb);
    }

    @Transactional
    public void generateForAllUsersAndMonth(YearMonth targetMonth) {
        List<User> users = userRepository.findAll();
        logger.info("Generating monthly balances for {} users for {}-{}", users.size(), targetMonth.getYear(), targetMonth.getMonthValue());
        for (User u : users) {
            generateForUserAndMonth(u.getUserId(), targetMonth);
        }
    }
}
