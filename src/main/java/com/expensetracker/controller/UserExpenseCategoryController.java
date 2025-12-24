package com.expensetracker.controller;

import com.expensetracker.dto.UserExpenseCategoryRequest;
import com.expensetracker.dto.UserExpenseCategoryResponse;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.UserExpensesRepository;
import com.expensetracker.service.UserExpenseCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-expense-category")
public class UserExpenseCategoryController {

    private final UserExpenseCategoryService userExpenseCategoryService;
    private final ExpenseRepository expenseRepository;
    private final UserExpensesRepository userExpensesRepository;

    @Autowired
    public UserExpenseCategoryController(UserExpenseCategoryService userExpenseCategoryService,
                                         ExpenseRepository expenseRepository,
                                         UserExpensesRepository userExpensesRepository) {
        this.userExpenseCategoryService = userExpenseCategoryService;
        this.expenseRepository = expenseRepository;
        this.userExpensesRepository = userExpensesRepository;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> findAll(@PathVariable String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        try {
            List<UserExpenseCategoryResponse> categories = userExpenseCategoryService.findAll(userId);
            return ResponseEntity.ok(categories);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @GetMapping("/{userId}/active")
    public ResponseEntity<?> findActive(@PathVariable String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        try {
            List<UserExpenseCategoryResponse> categories = userExpenseCategoryService.findActive(userId);
            return ResponseEntity.ok(categories);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @PostMapping("/{userId}")
    public ResponseEntity<?> add(@PathVariable String userId, @RequestBody UserExpenseCategoryRequest request) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        if (request == null || request.getUserExpenseCategoryName() == null || request.getUserExpenseCategoryName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userExpenseCategoryName required"));
        }
        try {
            UserExpenseCategoryResponse response = userExpenseCategoryService.add(
                    userId,
                    request.getUserExpenseCategoryName(),
                    request.getStatus()
            );
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @PutMapping("/{userId}/{id}")
    public ResponseEntity<?> update(@PathVariable String userId,
                                    @PathVariable Integer id,
                                    @RequestBody UserExpenseCategoryRequest request) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        if (id == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "id required"));
        }
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        }
        try {
            UserExpenseCategoryResponse response = userExpenseCategoryService.update(
                    userId,
                    id,
                    request.getUserExpenseCategoryName(),
                    request.getStatus()
            );
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @DeleteMapping("/{userId}/{id}")
    public ResponseEntity<?> delete(@PathVariable String userId, @PathVariable Integer id) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        if (id == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "id required"));
        }
        try {
            // check in expenses table
            boolean usedInExpenses = expenseRepository.existsByUserExpenseCategoryId(id);
            boolean usedInUserExpenses = userExpensesRepository.existsByUserExpenseCategoryId(id);
            if (usedInExpenses || usedInUserExpenses) {
                return ResponseEntity.badRequest().body(Map.of("error", "user expense category cannot be deleted as it is already mapped in the expenses."));
            }

            userExpenseCategoryService.delete(userId, id);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @PostMapping("/{userId}/copy-master")
    public ResponseEntity<?> copyMasterCategories(@PathVariable String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        try {
            userExpenseCategoryService.copyMasterCategoriesToUser(userId);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @PostMapping("/{userId}/id")
    public ResponseEntity<?> getIdByName(@PathVariable String userId, @RequestBody UserExpenseCategoryRequest userExpenseCategoryRequest) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        if (userExpenseCategoryRequest.getUserExpenseCategoryName() == null || userExpenseCategoryRequest.getUserExpenseCategoryName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name required"));
        }
        try {
            var opt = userExpenseCategoryService.findIdByUserIdAndName(userId, userExpenseCategoryRequest.getUserExpenseCategoryName());
            if (opt.isPresent()) {
                return ResponseEntity.ok(Map.of("userExpenseCategoryId", opt.get()));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "category not found"));
            }
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }
}
