package com.expensetracker.controller;

import com.expensetracker.dto.UserCreditCardEstimatesRequest;
import com.expensetracker.dto.UserCreditCardEstimatesResponse;
import com.expensetracker.service.UserCreditCardEstimatesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-credit-card-estimates")
public class UserCreditCardEstimatesController {

    private final UserCreditCardEstimatesService service;

    @Autowired
    public UserCreditCardEstimatesController(UserCreditCardEstimatesService service) {
        this.service = service;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> findAll(@PathVariable String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        List<UserCreditCardEstimatesResponse> list = service.findAll(userId);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{userId}")
    public ResponseEntity<?> add(@PathVariable String userId,
                                 @RequestBody UserCreditCardEstimatesRequest request) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        }
        UserCreditCardEstimatesResponse response = service.add(userId, request);
        return ResponseEntity.ok(Map.of("status", "success", "id", response.getUserCreditCardEstimatesId()));
    }

    @PutMapping("/{userId}/{id}")
    public ResponseEntity<?> update(@PathVariable String userId,
                                    @PathVariable Integer id,
                                    @RequestBody UserCreditCardEstimatesRequest request) {
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

