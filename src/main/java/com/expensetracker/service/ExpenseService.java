package com.expensetracker.service;

import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.dto.ExpenseResponse;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseCategory;
import com.expensetracker.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@CacheConfig(cacheNames = "expenses")
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryService expenseCategoryService;

    @Autowired
    public ExpenseService(ExpenseRepository expenseRepository, ExpenseCategoryService expenseCategoryService) {
        this.expenseRepository = expenseRepository;
        this.expenseCategoryService = expenseCategoryService;
    }

    public List<Expense> getExpensesByUsername(String username) {
        return expenseRepository.findByUsername(username);
    }

    @Cacheable(key = "#username")
    public List<ExpenseResponse> getExpenseResponsesByUsername(String username) {
        List<Expense> list = getExpensesByUsername(username);
        return mapToResponses(list);
    }

    public List<Expense> getExpensesByUsernameAndDateRange(String username, LocalDate start, LocalDate end) {
        return expenseRepository.findByUsernameAndExpenseDateBetween(username, start, end);
    }

    @Cacheable(key = "#username + ':' + #start + ':' + #end")
    public List<ExpenseResponse> getExpenseResponsesByUsernameAndDateRange(String username, LocalDate start, LocalDate end) {
        List<Expense> list = getExpensesByUsernameAndDateRange(username, start, end);
        return mapToResponses(list);
    }

    public List<Expense> getExpensesByUsernameForMonth(String username, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return getExpensesByUsernameAndDateRange(username, start, end);
    }

    @Cacheable(key = "#username + ':' + #year + ':' + #month")
    public List<ExpenseResponse> getExpenseResponsesByUsernameForMonth(String username, int year, int month) {
        List<Expense> list = getExpensesByUsernameForMonth(username, year, month);
        return mapToResponses(list);
    }

    public List<Expense> getExpensesByUsernameForYear(String username, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return getExpensesByUsernameAndDateRange(username, start, end);
    }

    @Cacheable(key = "#username + ':' + #year")
    public List<ExpenseResponse> getExpenseResponsesByUsernameForYear(String username, int year) {
        List<Expense> list = getExpensesByUsernameForYear(username, year);
        return mapToResponses(list);
    }

    private List<ExpenseResponse> mapToResponses(List<Expense> list) {
        List<ExpenseResponse> resp = new ArrayList<>();
        for (Expense e : list) {
            ExpenseResponse r = new ExpenseResponse();
            r.setExpensesId(e.getExpensesId());
            r.setUsername(e.getUsername());
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

    @CacheEvict(allEntries = true)
    public Expense addExpense(ExpenseRequest request) {
        Expense e = new Expense();
        e.setUsername(request.getUsername());
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

    @CacheEvict(allEntries = true)
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
        if (request.getUsername() != null && !request.getUsername().equals(e.getUsername())) {
            throw new IllegalArgumentException("username mismatch");
        }
        if (request.getExpenseName() != null) e.setExpenseName(request.getExpenseName());
        if (request.getExpenseAmount() != null) e.setExpenseAmount(request.getExpenseAmount());
        if (request.getExpenseCategoryId() != null) e.setExpenseCategoryId(request.getExpenseCategoryId());
        if (request.getExpenseDate() != null) e.setExpenseDate(request.getExpenseDate());
        e.setLastUpdateTmstp(LocalDateTime.now());
        return expenseRepository.save(e);
    }

    @CacheEvict(allEntries = true)
    public boolean deleteExpense(String username, Integer expensesId) {
        if (expensesId == null || username == null) {
            return false;
        }
        Optional<Expense> opt = expenseRepository.findById(expensesId);
        if (opt.isEmpty()) {
            return false;
        }
        Expense e = opt.get();
        if (e.getUsername() == null || !e.getUsername().equals(username)) {
            return false;
        }
        expenseRepository.deleteById(expensesId);
        return true;
    }

    // delete all expenses for a username using a single repository query
    public void deleteAllByUsername(String username) {
        if (username == null) return;
        expenseRepository.deleteByUsername(username);
    }
}
