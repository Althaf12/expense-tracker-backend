package com.expensetracker.service;

import com.expensetracker.dto.UserExpenseCategoryResponse;
import com.expensetracker.model.ExpenseCategory;
import com.expensetracker.model.UserExpenseCategory;
import com.expensetracker.repository.UserExpenseCategoryRepository;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.UserExpensesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(UserExpenseCategoryService.class);

    private final UserExpenseCategoryRepository userExpenseCategoryRepository;
    private final ExpenseCategoryService expenseCategoryService;
    private final ExpenseRepository expenseRepository;
    private final UserExpensesRepository userExpensesRepository;

    @Autowired
    public UserExpenseCategoryService(UserExpenseCategoryRepository userExpenseCategoryRepository,
                                      ExpenseCategoryService expenseCategoryService,
                                      ExpenseRepository expenseRepository,
                                      UserExpensesRepository userExpensesRepository) {
        this.userExpenseCategoryRepository = userExpenseCategoryRepository;
        this.expenseCategoryService = expenseCategoryService;
        this.expenseRepository = expenseRepository;
        this.userExpensesRepository = userExpensesRepository;
    }

    @Cacheable(key = "#userId")
    public List<UserExpenseCategoryResponse> findAll(String userId) {
        List<UserExpenseCategory> categories = userExpenseCategoryRepository.findByUserIdOrderByUserExpenseCategoryName(userId);
        return categories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(key = "#userId")
    public List<UserExpenseCategoryResponse> findActive(String userId) {
        List<UserExpenseCategory> categories = userExpenseCategoryRepository.findByUserIdAndStatusOrderByUserExpenseCategoryName(userId, "A");
        return categories.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenseCategories", key = "#userId"),
            @CacheEvict(cacheNames = "userExpenses", key = "#userId"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public UserExpenseCategoryResponse add(String userId, String categoryName, String status) {
        // Check count limit
        int count = userExpenseCategoryRepository.countByUserId(userId);
        if (count >= 20) {
            throw new IllegalArgumentException("User may have at most 20 categories");
        }

        UserExpenseCategory category = new UserExpenseCategory();
        category.setUserId(userId);
        category.setUserExpenseCategoryName(categoryName);
        category.setStatus(status != null && !status.isBlank() ? status : "A");
        category.setLastUpdateTmstp(LocalDateTime.now());

        UserExpenseCategory saved = userExpenseCategoryRepository.save(category);
        logger.info("Added category {} for userId: {}", categoryName, userId);
        return toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenseCategories", key = "#userId"),
            @CacheEvict(cacheNames = "userExpenses", key = "#userId"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public UserExpenseCategoryResponse update(String userId, Integer id, String newName, String newStatus) {
        Optional<UserExpenseCategory> opt = userExpenseCategoryRepository.findByUserExpenseCategoryIdAndUserId(id, userId);
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
        logger.info("Updated category {} for userId: {}", id, userId);
        return toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenseCategories", key = "#userId"),
            @CacheEvict(cacheNames = "userExpenses", key = "#userId"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public void delete(String userId, Integer id) {
        Optional<UserExpenseCategory> opt = userExpenseCategoryRepository.findByUserExpenseCategoryIdAndUserId(id, userId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("category not found");
        }
        userExpenseCategoryRepository.delete(opt.get());
        logger.info("Deleted category {} for userId: {}", id, userId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenseCategories", key = "#userId"),
            @CacheEvict(cacheNames = "userExpenses", key = "#userId"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public void deleteAll(String userId) {
        userExpenseCategoryRepository.deleteByUserId(userId);
        logger.info("Deleted all categories for userId: {}", userId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenseCategories", key = "#userId"),
            @CacheEvict(cacheNames = "userExpenses", key = "#userId"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public void copyMasterCategoriesToUser(String userId) {
        logger.info("Copying master categories to userId: {}", userId);
        List<ExpenseCategory> masterCategories = expenseCategoryService.findAll();

        List<UserExpenseCategory> existingUserCategories = userExpenseCategoryRepository.findByUserIdOrderByUserExpenseCategoryName(userId);

        List<Integer> referencedIdList = expenseRepository.findDistinctUserExpenseCategoryIdByUserId(userId);
        Set<Integer> referencedIds = referencedIdList == null ? Collections.emptySet() : referencedIdList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> mappedNames = new HashSet<>();
        if (!referencedIds.isEmpty()) {
            var referencedCategories = userExpenseCategoryRepository.findAllById(referencedIds);
            mappedNames = referencedCategories.stream()
                    .map(c -> c.getUserExpenseCategoryName() == null ? "" : c.getUserExpenseCategoryName().trim().toLowerCase())
                    .collect(Collectors.toSet());
        }

        for (UserExpenseCategory uc : existingUserCategories) {
            String ucNameNorm = uc.getUserExpenseCategoryName() == null ? "" : uc.getUserExpenseCategoryName().trim().toLowerCase();
            Integer ucId = uc.getUserExpenseCategoryId();
            boolean referencedInExpenses = referencedIds.contains(ucId) || mappedNames.contains(ucNameNorm);
            boolean referencedInUserExpenses = ucId != null && userExpensesRepository.existsByUserExpenseCategoryId(ucId);
            if (referencedInExpenses || referencedInUserExpenses) {
                continue;
            }
            userExpenseCategoryRepository.delete(uc);
        }

        List<UserExpenseCategory> currentUserCategories = userExpenseCategoryRepository.findByUserIdOrderByUserExpenseCategoryName(userId);
        Set<String> currentNames = currentUserCategories.stream()
                .map(c -> c.getUserExpenseCategoryName().trim().toLowerCase())
                .collect(Collectors.toSet());

        int availableSlots = 20 - currentUserCategories.size();
        if (availableSlots <= 0) return;

        for (ExpenseCategory master : masterCategories) {
            if (availableSlots <= 0) break;
            String masterNameNorm = master.getExpenseCategoryName().trim().toLowerCase();
            if (mappedNames.contains(masterNameNorm)) continue;
            if (currentNames.contains(masterNameNorm)) continue;

            UserExpenseCategory userCategory = new UserExpenseCategory();
            userCategory.setUserId(userId);
            userCategory.setUserExpenseCategoryName(master.getExpenseCategoryName());
            userCategory.setStatus("A");
            userCategory.setLastUpdateTmstp(LocalDateTime.now());
            userExpenseCategoryRepository.save(userCategory);
            availableSlots--;
            currentNames.add(masterNameNorm);
        }
        logger.info("Copied master categories to userId: {}", userId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenseCategories", key = "#userId"),
            @CacheEvict(cacheNames = "userExpenses", key = "#userId"),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public void onUserCreated(String userId) {
        logger.info("Initializing categories for new userId: {}", userId);
        List<ExpenseCategory> masterCategories = expenseCategoryService.findAll();

        int toInsert = Math.min(masterCategories.size(), 20);

        for (int i = 0; i < toInsert; i++) {
            ExpenseCategory master = masterCategories.get(i);
            UserExpenseCategory userCategory = new UserExpenseCategory();
            userCategory.setUserId(userId);
            userCategory.setUserExpenseCategoryName(master.getExpenseCategoryName());
            userCategory.setStatus("A");
            userCategory.setLastUpdateTmstp(LocalDateTime.now());
            userExpenseCategoryRepository.save(userCategory);
        }
        logger.info("Initialized {} categories for new userId: {}", toInsert, userId);
    }

    private UserExpenseCategoryResponse toResponse(UserExpenseCategory category) {
        UserExpenseCategoryResponse response = new UserExpenseCategoryResponse();
        response.setUserExpenseCategoryId(category.getUserExpenseCategoryId());
        response.setUserId(category.getUserId());
        response.setUserExpenseCategoryName(category.getUserExpenseCategoryName());
        response.setLastUpdateTmstp(category.getLastUpdateTmstp());
        response.setStatus(category.getStatus());
        return response;
    }

    public Optional<UserExpenseCategory> findById(Integer expenseCategoryId) {
        return userExpenseCategoryRepository.findById(expenseCategoryId);
    }

    public Optional<Integer> findIdByUserIdAndName(String userId, String name) {
        Optional<UserExpenseCategory> opt = userExpenseCategoryRepository.findByUserIdAndUserExpenseCategoryName(userId, name);
        return opt.map(UserExpenseCategory::getUserExpenseCategoryId);
    }
}
