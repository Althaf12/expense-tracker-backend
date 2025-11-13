package com.expensetracker.service;

import com.expensetracker.dto.UserExpenseResponse;
import com.expensetracker.model.UserExpense;
import com.expensetracker.repository.UserExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CacheConfig(cacheNames = {"userExpenses"})
@Service
public class UserExpenseService {

    private final UserExpenseRepository userExpenseRepository;

    @Autowired
    public UserExpenseService(UserExpenseRepository userExpenseRepository) {
        this.userExpenseRepository = userExpenseRepository;
    }

    @Cacheable(key = "#username")
    public List<UserExpenseResponse> findAll(String username) {
        List<UserExpense> expenses = userExpenseRepository.findByUsernameOrderByUserExpenseName(username);
        return expenses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(key = "#username")
    public List<UserExpenseResponse> findActive(String username) {
        List<UserExpense> expenses = userExpenseRepository.findByUsernameAndStatusOrderByUserExpenseName(username, "A");
        return expenses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenses", key = "#username"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public UserExpenseResponse add(String username, String userExpenseName, Integer userExpenseCategoryId, String status) {
        // Check count limit - max 100 user expenses per user
        int count = userExpenseRepository.countByUsername(username);
        if (count >= 100) {
            throw new IllegalArgumentException("User may have at most 100 user expenses");
        }

        UserExpense expense = new UserExpense();
        expense.setUsername(username);
        expense.setUserExpenseName(userExpenseName);
        expense.setUserExpenseCategoryId(userExpenseCategoryId);
        expense.setStatus(status != null && !status.isBlank() ? status : "A");
        expense.setLastUpdateTmstp(LocalDateTime.now());

        UserExpense saved = userExpenseRepository.save(expense);
        return toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenses", key = "#username"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public UserExpenseResponse update(String username, Integer id, String newName, Integer newCategoryId, String newStatus) {
        Optional<UserExpense> opt = userExpenseRepository.findByUserExpensesIdAndUsername(id, username);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("user expense not found");
        }

        UserExpense expense = opt.get();
        if (newName != null && !newName.isBlank()) {
            expense.setUserExpenseName(newName);
        }
        if (newCategoryId != null) {
            expense.setUserExpenseCategoryId(newCategoryId);
        }
        if (newStatus != null && !newStatus.isBlank()) {
            expense.setStatus(newStatus);
        }
        expense.setLastUpdateTmstp(LocalDateTime.now());

        UserExpense saved = userExpenseRepository.save(expense);
        return toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenses", key = "#username"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public void delete(String username, Integer id) {
        Optional<UserExpense> opt = userExpenseRepository.findByUserExpensesIdAndUsername(id, username);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("user expense not found");
        }
        userExpenseRepository.delete(opt.get());
    }

    private UserExpenseResponse toResponse(UserExpense expense) {
        UserExpenseResponse response = new UserExpenseResponse();
        response.setUserExpensesId(expense.getUserExpensesId());
        response.setUsername(expense.getUsername());
        response.setUserExpenseName(expense.getUserExpenseName());
        response.setUserExpenseCategoryId(expense.getUserExpenseCategoryId());
        response.setLastUpdateTmstp(expense.getLastUpdateTmstp());
        response.setStatus(expense.getStatus());
        return response;
    }
}
