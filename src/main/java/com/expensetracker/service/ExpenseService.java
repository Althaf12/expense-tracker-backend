package com.expensetracker.service;

import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Autowired
    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public List<Expense> getExpensesByUserId(String userId) {
        return expenseRepository.findByUserId(userId);
    }

    public List<Expense> getExpensesByUserIdAndDateRange(String userId, LocalDate start, LocalDate end) {
        return expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end);
    }

    public List<Expense> getExpensesByUserIdForMonth(String userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return getExpensesByUserIdAndDateRange(userId, start, end);
    }

    public List<Expense> getExpensesByUserIdForYear(String userId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return getExpensesByUserIdAndDateRange(userId, start, end);
    }

    public Expense addExpense(ExpenseRequest request) {
        Expense e = new Expense();
        e.setUserId(request.getUserId());
        e.setExpenseName(request.getExpenseName());
        e.setExpenseAmount(request.getExpenseAmount());
        e.setExpenseCategoryId(request.getExpenseCategoryId());
        e.setExpenseDate(request.getExpenseDate());
        // set timestamp; entity also sets in @PrePersist but set explicitly to be sure
        e.setLastUpdateTmstp(LocalDateTime.now());
        return expenseRepository.save(e);
    }

    public Optional<Expense> findById(Integer id) {
        return expenseRepository.findById(id);
    }

    public Expense updateExpense(ExpenseRequest request) {
        if (request.getExpensesId() == null) {
            throw new IllegalArgumentException("expensesId is required for update");
        }
        Optional<Expense> opt = expenseRepository.findById(request.getExpensesId());
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("expense not found");
        }
        Expense e = opt.get();
        // verify user matches
        if (request.getUserId() != null && !request.getUserId().equals(e.getUserId())) {
            throw new IllegalArgumentException("userId mismatch");
        }
        if (request.getExpenseName() != null) e.setExpenseName(request.getExpenseName());
        if (request.getExpenseAmount() != null) e.setExpenseAmount(request.getExpenseAmount());
        if (request.getExpenseCategoryId() != null) e.setExpenseCategoryId(request.getExpenseCategoryId());
        if (request.getExpenseDate() != null) e.setExpenseDate(request.getExpenseDate());
        e.setLastUpdateTmstp(LocalDateTime.now());
        return expenseRepository.save(e);
    }

    public boolean deleteExpense(String userId, Integer expensesId) {
        if (expensesId == null || userId == null) {
            return false;
        }
        Optional<Expense> opt = expenseRepository.findById(expensesId);
        if (opt.isEmpty()) {
            return false;
        }
        Expense e = opt.get();
        if (e.getUserId() == null || !e.getUserId().equals(userId)) {
            return false;
        }
        expenseRepository.deleteById(expensesId);
        return true;
    }
}
