package com.expensetracker.service;

import com.expensetracker.dto.UserExpenseCategoryResponse;
import com.expensetracker.model.ExpenseCategory;
import com.expensetracker.model.UserExpenseCategory;
import com.expensetracker.repository.UserExpenseCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CacheConfig(cacheNames = {"userExpenseCategories"})
@Service
public class UserExpenseCategoryService {

    private final UserExpenseCategoryRepository userExpenseCategoryRepository;
    private final ExpenseCategoryService expenseCategoryService;

    @Autowired
    public UserExpenseCategoryService(UserExpenseCategoryRepository userExpenseCategoryRepository,
                                      ExpenseCategoryService expenseCategoryService) {
        this.userExpenseCategoryRepository = userExpenseCategoryRepository;
        this.expenseCategoryService = expenseCategoryService;
    }

    @Cacheable(key = "#username")
    public List<UserExpenseCategoryResponse> findAll(String username) {
        List<UserExpenseCategory> categories = userExpenseCategoryRepository.findByUsernameOrderByUserExpenseCategoryName(username);
        return categories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(key = "#username")
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
    @CacheEvict(key = "#username")
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
    @CacheEvict(key = "#username")
    public void delete(String username, Integer id) {
        Optional<UserExpenseCategory> opt = userExpenseCategoryRepository.findByUserExpenseCategoryIdAndUsername(id, username);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("category not found");
        }
        userExpenseCategoryRepository.delete(opt.get());
    }

    @Transactional
    @CacheEvict(key = "#username")
    public void deleteAll(String username) {
        userExpenseCategoryRepository.deleteByUsername(username);
    }

    @Transactional
    @CacheEvict(key = "#username")
    public void copyMasterCategoriesToUser(String username) {
        // Delete all existing categories for the user
        userExpenseCategoryRepository.deleteByUsername(username);

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

    @Transactional
    @CacheEvict(key = "#username")
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
}
