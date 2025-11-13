package com.expensetracker.controller;

import com.expensetracker.dto.UserExpenseRequest;
import com.expensetracker.dto.UserExpenseResponse;
import com.expensetracker.service.UserExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-expenses")
public class UserExpensesController {

    private final UserExpenseService userExpenseService;

    @Autowired
    public UserExpensesController(UserExpenseService userExpenseService) {
        this.userExpenseService = userExpenseService;
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> findAll(@PathVariable String username) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username required"));
        }
        try {
            List<UserExpenseResponse> expenses = userExpenseService.findAll(username);
            return ResponseEntity.ok(expenses);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @GetMapping("/{username}/active")
    public ResponseEntity<?> findActive(@PathVariable String username) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username required"));
        }
        try {
            List<UserExpenseResponse> expenses = userExpenseService.findActive(username);
            return ResponseEntity.ok(expenses);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @PostMapping("/{username}")
    public ResponseEntity<?> add(@PathVariable String username, @RequestBody UserExpenseRequest request) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username required"));
        }
        if (request == null || request.getUserExpenseName() == null || request.getUserExpenseName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userExpenseName required"));
        }
        if (request.getUserExpenseCategoryId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userExpenseCategoryId required"));
        }
        try {
            UserExpenseResponse response = userExpenseService.add(
                    username,
                    request.getUserExpenseName(),
                    request.getUserExpenseCategoryId(),
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
                                    @RequestBody UserExpenseRequest request) {
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
            UserExpenseResponse response = userExpenseService.update(
                    username,
                    id,
                    request.getUserExpenseName(),
                    request.getUserExpenseCategoryId(),
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
            userExpenseService.delete(username, id);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }
}
