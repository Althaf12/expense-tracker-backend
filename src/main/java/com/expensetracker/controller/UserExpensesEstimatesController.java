package com.expensetracker.controller;

import com.expensetracker.dto.UserExpensesEstimatesRequest;
import com.expensetracker.dto.UserExpensesEstimatesResponse;
import com.expensetracker.service.UserExpensesEstimatesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-expenses-estimates")
public class UserExpensesEstimatesController {

    private final UserExpensesEstimatesService service;

    @Autowired
    public UserExpensesEstimatesController(UserExpensesEstimatesService service) {
        this.service = service;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> findAll(@PathVariable String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        List<UserExpensesEstimatesResponse> list = service.findAll(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{userId}/active")
    public ResponseEntity<?> findActive(@PathVariable String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        List<UserExpensesEstimatesResponse> list = service.findActive(userId);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{userId}")
    public ResponseEntity<?> add(@PathVariable String userId,
                                 @RequestBody UserExpensesEstimatesRequest request) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        }
        UserExpensesEstimatesResponse response = service.add(userId, request);
        return ResponseEntity.ok(Map.of("status", "success", "id", response.getUserExpensesEstimatesId()));
    }

    @PutMapping("/{userId}/{id}")
    public ResponseEntity<?> update(@PathVariable String userId,
                                    @PathVariable Integer id,
                                    @RequestBody UserExpensesEstimatesRequest request) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        }
        service.update(userId, id, request);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @DeleteMapping("/{userId}/{id}")
    public ResponseEntity<?> delete(@PathVariable String userId,
                                    @PathVariable Integer id) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        service.delete(userId, id);
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}

