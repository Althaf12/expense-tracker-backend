package com.expensetracker.service;

import com.expensetracker.dto.UserExpenseCategoryResponse;
import com.expensetracker.model.ExpenseCategory;
import com.expensetracker.model.UserExpenseCategory;
import com.expensetracker.repository.UserExpenseCategoryRepository;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.model.Expense;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@CacheConfig(cacheNames = {"userExpenseCategories"})
@Service
public class UserExpenseCategoryService {

    private final UserExpenseCategoryRepository userExpenseCategoryRepository;
    private final ExpenseCategoryService expenseCategoryService;
    private final ExpenseRepository expenseRepository;

    @Autowired
    public UserExpenseCategoryService(UserExpenseCategoryRepository userExpenseCategoryRepository,
                                      ExpenseCategoryService expenseCategoryService,
                                      ExpenseRepository expenseRepository) {
        this.userExpenseCategoryRepository = userExpenseCategoryRepository;
        this.expenseCategoryService = expenseCategoryService;
        this.expenseRepository = expenseRepository;
    }

    @Cacheable(key = "#username")
    public List<UserExpenseCategoryResponse> findAll(String username) {
        List<UserExpenseCategory> categories = userExpenseCategoryRepository.findByUsernameOrderByUserExpenseCategoryName(username);
        return categories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(key = "#username")
    public List<UserExpenseCategoryResponse> findActive(String username) {
        List<UserExpenseCategory> categories = userExpenseCategoryRepository.findByUsernameAndStatusOrderByUserExpenseCategoryName(username, "A");
        return categories.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenseCategories", key = "#username"),
            @CacheEvict(cacheNames = "userExpenses", key = "#username"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public UserExpenseCategoryResponse add(String username, String categoryName, String status) {
        // Check count limit
        int count = userExpenseCategoryRepository.countByUsername(username);
        if (count >= 20) {
            throw new IllegalArgumentException("User may have at most 20 categories");
        }

        UserExpenseCategory category = new UserExpenseCategory();
        category.setUsername(username);
        category.setUserExpenseCategoryName(categoryName);
        category.setStatus(status != null && !status.isBlank() ? status : "A");
        category.setLastUpdateTmstp(LocalDateTime.now());

        UserExpenseCategory saved = userExpenseCategoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenseCategories", key = "#username"),
            @CacheEvict(cacheNames = "userExpenses", key = "#username"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public UserExpenseCategoryResponse update(String username, Integer id, String newName, String newStatus) {
        Optional<UserExpenseCategory> opt = userExpenseCategoryRepository.findByUserExpenseCategoryIdAndUsername(id, username);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("category not found");
        }

        UserExpenseCategory category = opt.get();
        if (newName != null && !newName.isBlank()) {
            category.setUserExpenseCategoryName(newName);
        }
        if (newStatus != null && !newStatus.isBlank()) {
            category.setStatus(newStatus);
        }
        category.setLastUpdateTmstp(LocalDateTime.now());

        UserExpenseCategory saved = userExpenseCategoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenseCategories", key = "#username"),
            @CacheEvict(cacheNames = "userExpenses", key = "#username"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public void delete(String username, Integer id) {
        Optional<UserExpenseCategory> opt = userExpenseCategoryRepository.findByUserExpenseCategoryIdAndUsername(id, username);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("category not found");
        }
        userExpenseCategoryRepository.delete(opt.get());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenseCategories", key = "#username"),
            @CacheEvict(cacheNames = "userExpenses", key = "#username"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public void deleteAll(String username) {
        userExpenseCategoryRepository.deleteByUsername(username);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenseCategories", key = "#username"),
            @CacheEvict(cacheNames = "userExpenses", key = "#username"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public void copyMasterCategoriesToUser(String username) {
        // Get master categories
        List<ExpenseCategory> masterCategories = expenseCategoryService.findAll();

        // Get existing user categories for the username
        List<UserExpenseCategory> existingUserCategories = userExpenseCategoryRepository.findByUsernameOrderByUserExpenseCategoryName(username);

        // Get category names referenced in expenses for this user (normalized)
        List<Expense> userExpenses = expenseRepository.findByUsername(username);
        Set<String> mappedNames = userExpenses.stream()
                .map(Expense::getUserExpenseCategoryId)
                .filter(Objects::nonNull)
                .map(id -> userExpenseCategoryRepository.findById(id))
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getUserExpenseCategoryName().trim().toLowerCase())
                .collect(Collectors.toSet());

        // Delete existing user categories that are NOT referenced in expenses (by name)
        for (UserExpenseCategory uc : existingUserCategories) {
            String ucNameNorm = uc.getUserExpenseCategoryName() == null ? "" : uc.getUserExpenseCategoryName().trim().toLowerCase();
            if (!mappedNames.contains(ucNameNorm)) {
                userExpenseCategoryRepository.delete(uc);
            }
        }

        // Refresh existing names after deletion
        List<UserExpenseCategory> currentUserCategories = userExpenseCategoryRepository.findByUsernameOrderByUserExpenseCategoryName(username);
        Set<String> currentNames = currentUserCategories.stream()
                .map(c -> c.getUserExpenseCategoryName().trim().toLowerCase())
                .collect(Collectors.toSet());

        // Limit to 20 categories total
        int availableSlots = 20 - currentUserCategories.size();
        if (availableSlots <= 0) return;

        // Insert up to availableSlots master categories that are not mapped in expenses and not already present
        for (ExpenseCategory master : masterCategories) {
            if (availableSlots <= 0) break;
            String masterNameNorm = master.getExpenseCategoryName().trim().toLowerCase();
            if (mappedNames.contains(masterNameNorm)) continue; // already mapped in expenses
            if (currentNames.contains(masterNameNorm)) continue; // already present

            UserExpenseCategory userCategory = new UserExpenseCategory();
            userCategory.setUsername(username);
            userCategory.setUserExpenseCategoryName(master.getExpenseCategoryName());
            userCategory.setStatus("A");
            userCategory.setLastUpdateTmstp(LocalDateTime.now());
            userExpenseCategoryRepository.save(userCategory);
            availableSlots--;
            currentNames.add(masterNameNorm);
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenseCategories", key = "#username"),
            @CacheEvict(cacheNames = "userExpenses", key = "#username"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public void onUserCreated(String username) {
        // Get master categories
        List<ExpenseCategory> masterCategories = expenseCategoryService.findAll();

        // Limit to 20 categories
        int toInsert = Math.min(masterCategories.size(), 20);

        // Insert up to 20 master categories with status 'A'
        for (int i = 0; i < toInsert; i++) {
            ExpenseCategory master = masterCategories.get(i);
            UserExpenseCategory userCategory = new UserExpenseCategory();
            userCategory.setUsername(username);
            userCategory.setUserExpenseCategoryName(master.getExpenseCategoryName());
            userCategory.setStatus("A");
            userCategory.setLastUpdateTmstp(LocalDateTime.now());
            userExpenseCategoryRepository.save(userCategory);
        }
    }

    private UserExpenseCategoryResponse toResponse(UserExpenseCategory category) {
        UserExpenseCategoryResponse response = new UserExpenseCategoryResponse();
        response.setUserExpenseCategoryId(category.getUserExpenseCategoryId());
        response.setUsername(category.getUsername());
        response.setUserExpenseCategoryName(category.getUserExpenseCategoryName());
        response.setLastUpdateTmstp(category.getLastUpdateTmstp());
        response.setStatus(category.getStatus());
        return response;
    }

    public Optional<UserExpenseCategory> findById(Integer expenseCategoryId) {
        return userExpenseCategoryRepository.findById(expenseCategoryId);
    }

    // new helper to find id by username and name
    public Optional<Integer> findIdByUsernameAndName(String username, String name) {
        Optional<UserExpenseCategory> opt = userExpenseCategoryRepository.findByUsernameAndUserExpenseCategoryName(username, name);
        return opt.map(UserExpenseCategory::getUserExpenseCategoryId);
    }
}
