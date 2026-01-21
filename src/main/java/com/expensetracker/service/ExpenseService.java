package com.expensetracker.service;

import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.dto.ExpenseResponse;
import com.expensetracker.model.Expense;
import com.expensetracker.model.UserExpenseCategory;
import com.expensetracker.repository.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.expensetracker.util.Constants;

@CacheConfig(cacheNames = "expenses")
@Service
public class ExpenseService {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseService.class);

    private final ExpenseRepository expenseRepository;
    private final UserExpenseCategoryService userExpenseCategoryService;
    // use centralized constants for allowed page sizes

    @Autowired
    public ExpenseService(ExpenseRepository expenseRepository, UserExpenseCategoryService userExpenseCategoryService) {
        this.expenseRepository = expenseRepository;
        this.userExpenseCategoryService = userExpenseCategoryService;
    }

    public List<Expense> getExpensesByUserId(String userId) {
        return expenseRepository.findByUserId(userId);
    }

    @Cacheable(key = "#userId")
    public List<ExpenseResponse> getExpenseResponsesByUserId(String userId) {
        List<Expense> list = getExpensesByUserId(userId);
        return mapToResponses(list);
    }

    public List<Expense> getExpensesByUserIdAndDateRange(String userId, LocalDate start, LocalDate end) {
        return expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end);
    }

    @Cacheable(key = "#userId + ':' + #start + ':' + #end")
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

    @Cacheable(key = "#userId + ':' + #year + ':' + #month")
    public List<ExpenseResponse> getExpenseResponsesByUserIdForMonth(String userId, int year, int month) {
        List<Expense> list = getExpensesByUserIdForMonth(userId, year, month);
        return mapToResponses(list);
    }

    public List<Expense> getExpensesByUserIdForYear(String userId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return getExpensesByUserIdAndDateRange(userId, start, end);
    }

    @Cacheable(key = "#userId + ':' + #year")
    public List<ExpenseResponse> getExpenseResponsesByUserIdForYear(String userId, int year) {
        List<Expense> list = getExpensesByUserIdForYear(userId, year);
        return mapToResponses(list);
    }

    public Page<ExpenseResponse> getExpenseResponsesByUserId(String userId, int page, int size) {
        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) throw new IllegalArgumentException("invalid page size");
        PageRequest pr = PageRequest.of(Math.max(0, page), size);
        Page<Expense> p = expenseRepository.findByUserId(userId, pr);
        List<ExpenseResponse> content = mapToResponses(p.getContent());
        return new PageImpl<>(content, pr, p.getTotalElements());
    }

    @Cacheable(key = "#userId + ':' + #start + ':' + #end + ':' + #page + ':' + #size")
    public Page<ExpenseResponse> getExpenseResponsesByUserIdAndDateRange(String userId, LocalDate start, LocalDate end, int page, int size) {
        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) throw new IllegalArgumentException("invalid page size");
        PageRequest pr = PageRequest.of(Math.max(0, page), size);
        Page<Expense> p = expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end, pr);
        List<ExpenseResponse> content = mapToResponses(p.getContent());
        return new PageImpl<>(content, pr, p.getTotalElements());
    }

    public Page<ExpenseResponse> getExpenseResponsesByUserIdForMonth(String userId, int year, int month, int page, int size) {
        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) throw new IllegalArgumentException("invalid page size");
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return getExpenseResponsesByUserIdAndDateRange(userId, start, end, page, size);
    }

    public Page<ExpenseResponse> getExpenseResponsesByUserIdForYear(String userId, int year, int page, int size) {
        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) throw new IllegalArgumentException("invalid page size");
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return getExpenseResponsesByUserIdAndDateRange(userId, start, end, page, size);
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
            if (e.getUserExpenseCategoryId() != null) {
                Optional<UserExpenseCategory> catOpt = userExpenseCategoryService.findById(e.getUserExpenseCategoryId());
                if (catOpt.isPresent()) catName = catOpt.get().getUserExpenseCategoryName();
            }
            r.setUserExpenseCategoryName(catName);
            resp.add(r);
        }
        return resp;
    }

    @CacheEvict(allEntries = true)
    public Expense addExpense(ExpenseRequest request) {
        logger.info("Adding expense for userId: {}", request.getUserId());
        Expense e = new Expense();
        e.setUserId(request.getUserId());
        e.setExpenseName(request.getExpenseName());
        e.setExpenseAmount(request.getExpenseAmount());
        e.setUserExpenseCategoryId(request.getUserExpenseCategoryId());
        e.setExpenseDate(request.getExpenseDate());
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
        if (request.getUserId() != null && !request.getUserId().equals(e.getUserId())) {
            throw new IllegalArgumentException("userId mismatch");
        }
        if (request.getExpenseName() != null) e.setExpenseName(request.getExpenseName());
        if (request.getExpenseAmount() != null) e.setExpenseAmount(request.getExpenseAmount());
        if (request.getUserExpenseCategoryId() != null) e.setUserExpenseCategoryId(request.getUserExpenseCategoryId());
        if (request.getExpenseDate() != null) e.setExpenseDate(request.getExpenseDate());
        e.setLastUpdateTmstp(LocalDateTime.now());
        logger.info("Updated expense {} for userId: {}", request.getExpensesId(), e.getUserId());
        return expenseRepository.save(e);
    }

    @CacheEvict(allEntries = true)
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
        logger.info("Deleted expense {} for userId: {}", expensesId, userId);
        return true;
    }

    @CacheEvict(allEntries = true)
    public void deleteAllByUserId(String userId) {
        if (userId == null) return;
        logger.info("Deleting all expenses for userId: {}", userId);
        expenseRepository.deleteByUserId(userId);
    }
}
