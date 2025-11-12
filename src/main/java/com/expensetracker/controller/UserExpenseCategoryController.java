package com.expensetracker.controller;

import com.expensetracker.dto.UserExpenseCategoryRequest;
import com.expensetracker.dto.UserExpenseCategoryResponse;
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

    @Autowired
    public UserExpenseCategoryController(UserExpenseCategoryService userExpenseCategoryService) {
        this.userExpenseCategoryService = userExpenseCategoryService;
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> findAll(@PathVariable String username) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username required"));
        }
        try {
            List<UserExpenseCategoryResponse> categories = userExpenseCategoryService.findAll(username);
            return ResponseEntity.ok(categories);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @PostMapping("/{username}")
    public ResponseEntity<?> add(@PathVariable String username, @RequestBody UserExpenseCategoryRequest request) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username required"));
        }
        if (request == null || request.getUserExpenseCategoryName() == null || request.getUserExpenseCategoryName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userExpenseCategoryName required"));
        }
        try {
            UserExpenseCategoryResponse response = userExpenseCategoryService.add(
                    username,
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

    @PutMapping("/{username}/{id}")
    public ResponseEntity<?> update(@PathVariable String username,
                                    @PathVariable Integer id,
                                    @RequestBody UserExpenseCategoryRequest request) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username required"));
        }
        if (id == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "id required"));
        }
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        }
        try {
            UserExpenseCategoryResponse response = userExpenseCategoryService.update(
                    username,
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

    @DeleteMapping("/{username}/{id}")
    public ResponseEntity<?> delete(@PathVariable String username, @PathVariable Integer id) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username required"));
        }
        if (id == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "id required"));
        }
        try {
            userExpenseCategoryService.delete(username, id);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<?> deleteAll(@PathVariable String username) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username required"));
        }
        try {
            userExpenseCategoryService.deleteAll(username);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @PostMapping("/{username}/copy-master")
    public ResponseEntity<?> copyMasterCategories(@PathVariable String username) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username required"));
        }
        try {
            userExpenseCategoryService.copyMasterCategoriesToUser(username);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }
}
