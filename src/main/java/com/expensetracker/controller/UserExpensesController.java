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

    @GetMapping("/{username}")
    public ResponseEntity<?> findAll(@PathVariable String username) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username required"));
        }
        try {
            List<UserExpensesResponse> expenses = userExpensesService.findAll(username);
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
            List<UserExpensesResponse> expenses = userExpensesService.findActive(username);
            return ResponseEntity.ok(expenses);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @PostMapping("/{username}")
    public ResponseEntity<?> add(@PathVariable String username, @RequestBody UserExpensesRequest request) {
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
            UserExpensesResponse response = userExpensesService.add(
                    username,
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

    @PutMapping("/{username}/{id}")
    public ResponseEntity<?> update(@PathVariable String username,
                                    @PathVariable Integer id,
                                    @RequestBody UserExpensesRequest request) {
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
            UserExpensesResponse response = userExpensesService.update(
                    username,
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

    @DeleteMapping("/{username}/{id}")
    public ResponseEntity<?> delete(@PathVariable String username, @PathVariable Integer id) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username required"));
        }
        if (id == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "id required"));
        }
        try {
            userExpensesService.delete(username, id);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }
}
