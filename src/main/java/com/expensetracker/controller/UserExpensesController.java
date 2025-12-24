package com.expensetracker.controller;

import com.expensetracker.dto.UserExpensesRequest;
import com.expensetracker.dto.UserExpensesResponse;
import com.expensetracker.service.UserExpensesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-expenses")
public class UserExpensesController {

    private final UserExpensesService userExpensesService;

    @Autowired
    public UserExpensesController(UserExpensesService userExpensesService) {
        this.userExpensesService = userExpensesService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> findAll(@PathVariable String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        try {
            List<UserExpensesResponse> expenses = userExpensesService.findAll(userId);
            return ResponseEntity.ok(expenses);
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
            List<UserExpensesResponse> expenses = userExpensesService.findActive(userId);
            return ResponseEntity.ok(expenses);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @PostMapping("/{userId}")
    public ResponseEntity<?> add(@PathVariable String userId, @RequestBody UserExpensesRequest request) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        if (request == null || request.getUserExpenseName() == null || request.getUserExpenseName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userExpenseName required"));
        }
        if (request.getUserExpenseCategoryId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userExpenseCategoryId required"));
        }
        try {
            UserExpensesResponse response = userExpensesService.add(
                    userId,
                    request.getUserExpenseName(),
                    request.getUserExpenseCategoryId(),
                    request.getAmount(),
                    request.getPaid(),
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
                                    @RequestBody UserExpensesRequest request) {
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
            UserExpensesResponse response = userExpensesService.update(
                    userId,
                    id,
                    request.getUserExpenseName(),
                    request.getUserExpenseCategoryId(),
                    request.getAmount(),
                    request.getPaid(),
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
            userExpensesService.delete(userId, id);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }
}
