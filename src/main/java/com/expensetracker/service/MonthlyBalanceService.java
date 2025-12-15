package com.expensetracker.service;

import com.expensetracker.model.MonthlyBalance;
import com.expensetracker.model.Income;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.MonthlyBalanceRepository;
import com.expensetracker.repository.IncomeRepository;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.model.User;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class MonthlyBalanceService {

    private final MonthlyBalanceRepository monthlyBalanceRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public MonthlyBalanceService(MonthlyBalanceRepository monthlyBalanceRepository,
                                 IncomeRepository incomeRepository,
                                 ExpenseRepository expenseRepository,
                                 UserRepository userRepository) {
        this.monthlyBalanceRepository = monthlyBalanceRepository;
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    public Optional<MonthlyBalance> findLatestForUser(String username) {
        return monthlyBalanceRepository.findTopByUsernameOrderByYearDescMonthDesc(username);
    }

    public Optional<MonthlyBalance> findByUsernameYearMonth(String username, int year, int month) {
        return monthlyBalanceRepository.findByUsernameAndYearAndMonth(username, year, month);
    }

    private double totalIncomeForMonth(String username, YearMonth ym) {
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<Income> incomes = incomeRepository.findByUsernameAndReceivedDateBetween(username, start, end);
        return incomes.stream().mapToDouble(i -> i.getAmount() == null ? 0.0 : i.getAmount()).sum();
    }

    private double totalExpensesForMonth(String username, YearMonth ym) {
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<Expense> expenses = expenseRepository.findByUsernameAndExpenseDateBetween(username, start, end);
        return expenses.stream().mapToDouble(e -> e.getExpenseAmount() == null ? 0.0 : e.getExpenseAmount()).sum();
    }

    private double previousMonthClosingBalance(String username, YearMonth targetMonth) {
        // previous month
        YearMonth prev = targetMonth.minusMonths(1);
        Optional<MonthlyBalance> prevBalance = monthlyBalanceRepository.findByUsernameAndYearAndMonth(username, prev.getYear(), prev.getMonthValue());
        return prevBalance.map(MonthlyBalance::getClosingBalance).orElse(0.0);
    }

    @Transactional
    public MonthlyBalance generateForUserAndMonth(String username, YearMonth targetMonth) {
        // ensure idempotency
        Optional<MonthlyBalance> existing = monthlyBalanceRepository.findByUsernameAndYearAndMonth(username, targetMonth.getYear(), targetMonth.getMonthValue());
        if (existing.isPresent()) {
            return existing.get();
        }

        double opening = previousMonthClosingBalance(username, targetMonth);
        double income = totalIncomeForMonth(username, targetMonth);
        double expenses = totalExpensesForMonth(username, targetMonth);
        double closing = opening + income - expenses;

        MonthlyBalance mb = new MonthlyBalance();
        mb.setUsername(username);
        mb.setYear(targetMonth.getYear());
        mb.setMonth(targetMonth.getMonthValue());
        mb.setOpeningBalance(opening);
        mb.setClosingBalance(closing);

        return monthlyBalanceRepository.save(mb);
    }

    @Transactional
    public void generateForAllUsersAndMonth(YearMonth targetMonth) {
        List<User> users = userRepository.findAll();
        for (User u : users) {
            generateForUserAndMonth(u.getUsername(), targetMonth);
        }
    }
}

