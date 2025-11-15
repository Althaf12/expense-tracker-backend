package com.expensetracker.service;

import com.expensetracker.dto.UserExpensesResponse;
import com.expensetracker.model.UserExpenses;
import com.expensetracker.model.UserExpenseCategory;
import com.expensetracker.repository.UserExpenseCategoryRepository;
import com.expensetracker.repository.UserExpensesRepository;
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
public class UserExpensesService {

    private final UserExpensesRepository userExpensesRepository;
    private final UserExpenseCategoryRepository userExpenseCategoryRepository;

    @Autowired
    public UserExpensesService(UserExpensesRepository userExpensesRepository,
                               UserExpenseCategoryRepository userExpenseCategoryRepository) {
        this.userExpensesRepository = userExpensesRepository;
        this.userExpenseCategoryRepository = userExpenseCategoryRepository;
    }

    @Cacheable(key = "#username")
    public List<UserExpensesResponse> findAll(String username) {
        List<UserExpenses> expenses = userExpensesRepository.findByUsernameOrderByUserExpenseName(username);
        return expenses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(key = "#username")
    public List<UserExpensesResponse> findActive(String username) {
        List<UserExpenses> expenses = userExpensesRepository.findByUsernameAndStatusOrderByUserExpenseName(username, "A");
        return expenses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenses", key = "#username"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public UserExpensesResponse add(String username, String userExpenseName, Integer userExpenseCategoryId, Double amount, String paid, String status) {
        // Check count limit - max 100 user expenses per user
        int count = userExpensesRepository.countByUsername(username);
        if (count >= 100) {
            throw new IllegalArgumentException("User may have at most 100 user expenses");
        }

        UserExpenses expense = new UserExpenses();
        expense.setUsername(username);
        expense.setUserExpenseName(userExpenseName);
        expense.setUserExpenseCategoryId(userExpenseCategoryId);
        expense.setAmount(amount);
        expense.setPaid(paid);
        expense.setStatus(status != null && !status.isBlank() ? status : "A");
        expense.setLastUpdateTmstp(LocalDateTime.now());

        UserExpenses saved = userExpensesRepository.save(expense);
        return toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenses", key = "#username"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public UserExpensesResponse update(String username, Integer id, String newName, Integer newCategoryId, Double newAmount, String paid, String newStatus) {
        Optional<UserExpenses> opt = userExpensesRepository.findByUserExpensesIdAndUsername(id, username);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("user expense not found");
        }

        UserExpenses expense = opt.get();
        if (newName != null && !newName.isBlank()) {
            expense.setUserExpenseName(newName);
        }
        if (newCategoryId != null) {
            expense.setUserExpenseCategoryId(newCategoryId);
        }
        if (newAmount != null) {
            expense.setAmount(newAmount);
        }
        if (newStatus != null && !newStatus.isBlank()) {
            expense.setStatus(newStatus);
        }
        if (paid != null && !paid.isBlank()) {
            expense.setPaid(paid);
        }
        expense.setLastUpdateTmstp(LocalDateTime.now());

        UserExpenses saved = userExpensesRepository.save(expense);
        return toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenses", key = "#username"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public void delete(String username, Integer id) {
        Optional<UserExpenses> opt = userExpensesRepository.findByUserExpensesIdAndUsername(id, username);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("user expense not found");
        }
        userExpensesRepository.delete(opt.get());
    }

    private UserExpensesResponse toResponse(UserExpenses expense) {
        UserExpensesResponse response = new UserExpensesResponse();
        response.setUserExpensesId(expense.getUserExpensesId());
        response.setUsername(expense.getUsername());
        response.setUserExpenseName(expense.getUserExpenseName());

        String catName = null;
        if (expense.getUserExpenseCategoryId() != null) {
            Optional<UserExpenseCategory> catOpt = userExpenseCategoryRepository.findById(expense.getUserExpenseCategoryId());
            if (catOpt.isPresent()) catName = catOpt.get().getUserExpenseCategoryName();
        }
        response.setUserExpenseCategoryName(catName);

        response.setAmount(expense.getAmount());
        response.setPaid(expense.getPaid());
        response.setLastUpdateTmstp(expense.getLastUpdateTmstp());
        response.setStatus(expense.getStatus());
        return response;
    }
}
