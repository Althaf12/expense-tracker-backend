package com.expensetracker.service;

import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.dto.ExpenseResponse;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseCategory;
import com.expensetracker.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryService expenseCategoryService;

    @Autowired
    public ExpenseService(ExpenseRepository expenseRepository, ExpenseCategoryService expenseCategoryService) {
        this.expenseRepository = expenseRepository;
        this.expenseCategoryService = expenseCategoryService;
    }

    public List<Expense> getExpensesByUserId(String userId) {
        return expenseRepository.findByUserId(userId);
    }

    public List<ExpenseResponse> getExpenseResponsesByUserId(String userId) {
        List<Expense> list = getExpensesByUserId(userId);
        return mapToResponses(list);
    }

    public List<Expense> getExpensesByUserIdAndDateRange(String userId, LocalDate start, LocalDate end) {
        return expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end);
    }

    public List<ExpenseResponse> getExpenseResponsesByUserIdAndDateRange(String userId, LocalDate start, LocalDate end) {
        List<Expense> list = getExpensesByUserIdAndDateRange(userId, start, end);
        return mapToResponses(list);
    }

    public List<Expense> getExpensesByUserIdForMonth(String userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return getExpensesByUserIdAndDateRange(userId, start, end);
    }

    public List<ExpenseResponse> getExpenseResponsesByUserIdForMonth(String userId, int year, int month) {
        List<Expense> list = getExpensesByUserIdForMonth(userId, year, month);
        return mapToResponses(list);
    }

    public List<Expense> getExpensesByUserIdForYear(String userId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return getExpensesByUserIdAndDateRange(userId, start, end);
    }

    public List<ExpenseResponse> getExpenseResponsesByUserIdForYear(String userId, int year) {
        List<Expense> list = getExpensesByUserIdForYear(userId, year);
        return mapToResponses(list);
    }

    private List<ExpenseResponse> mapToResponses(List<Expense> list) {
        List<ExpenseResponse> resp = new ArrayList<>();
        for (Expense e : list) {
            ExpenseResponse r = new ExpenseResponse();
            r.setExpensesId(e.getExpensesId());
            r.setUserId(e.getUserId());
            r.setExpenseName(e.getExpenseName());
            r.setExpenseAmount(e.getExpenseAmount());
            r.setLastUpdateTmstp(e.getLastUpdateTmstp());
            r.setExpenseDate(e.getExpenseDate());
            // resolve category name
            String catName = null;
            if (e.getExpenseCategoryId() != null) {
                Optional<ExpenseCategory> catOpt = expenseCategoryService.findById(e.getExpenseCategoryId());
                if (catOpt.isPresent()) catName = catOpt.get().getExpenseCategoryName();
            }
            r.setExpenseCategoryName(catName);
            resp.add(r);
        }
        return resp;
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
