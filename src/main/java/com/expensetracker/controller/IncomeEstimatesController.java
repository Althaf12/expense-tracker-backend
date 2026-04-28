package com.expensetracker.controller;

import com.expensetracker.dto.IncomeEstimatesRequest;
import com.expensetracker.dto.IncomeEstimatesResponse;
import com.expensetracker.service.IncomeEstimatesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/income-estimates")
public class IncomeEstimatesController {

    private static final Logger logger = LoggerFactory.getLogger(IncomeEstimatesController.class);

    private final IncomeEstimatesService incomeEstimatesService;

    @Autowired
    public IncomeEstimatesController(IncomeEstimatesService incomeEstimatesService) {
        this.incomeEstimatesService = incomeEstimatesService;
    }

    /**
     * GET /api/income-estimates/{userId}
     * Returns all income estimates for the user, ordered by year desc, month asc.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> findAll(@PathVariable String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        List<IncomeEstimatesResponse> list = incomeEstimatesService.findAll(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * GET /api/income-estimates/{userId}/{month}/{year}
     * Returns income estimates for a specific month and year.
     */
    @GetMapping("/{userId}/{month}/{year}")
    public ResponseEntity<?> findByMonthAndYear(@PathVariable String userId,
                                                @PathVariable String month,
                                                @PathVariable Integer year) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        if (month == null || month.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "month required"));
        }
        if (year == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "year required"));
        }
        List<IncomeEstimatesResponse> list = incomeEstimatesService.findByMonthAndYear(userId, month, year);
        return ResponseEntity.ok(list);
    }

    /**
     * POST /api/income-estimates/{userId}
     * Adds a new income estimate for the user.
     */
    @PostMapping("/{userId}")
    public ResponseEntity<?> add(@PathVariable String userId,
                                 @RequestBody IncomeEstimatesRequest request) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        }
        IncomeEstimatesResponse response = incomeEstimatesService.add(userId, request);
        return ResponseEntity.ok(Map.of("status", "success", "id", response.getIncomeEstimatesId()));
    }

    /**
     * PUT /api/income-estimates/{userId}/{id}
     * Updates an existing income estimate.
     */
    @PutMapping("/{userId}/{id}")
    public ResponseEntity<?> update(@PathVariable String userId,
                                    @PathVariable Integer id,
                                    @RequestBody IncomeEstimatesRequest request) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        }
        incomeEstimatesService.update(userId, id, request);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    /**
     * DELETE /api/income-estimates/{userId}/{id}
     * Deletes an income estimate.
     */
    @DeleteMapping("/{userId}/{id}")
    public ResponseEntity<?> delete(@PathVariable String userId,
                                    @PathVariable Integer id) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        incomeEstimatesService.delete(userId, id);
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}

