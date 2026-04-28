package com.expensetracker.controller;

import com.expensetracker.service.UserExpensesEstimatesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/estimates")
public class AdminEstimatesController {

    private static final Logger logger = LoggerFactory.getLogger(AdminEstimatesController.class);

    private final UserExpensesEstimatesService userExpensesEstimatesService;

    public AdminEstimatesController(UserExpensesEstimatesService userExpensesEstimatesService) {
        this.userExpensesEstimatesService = userExpensesEstimatesService;
    }

    /**
     * Temporary admin endpoint to trigger the monthly estimates -> user_expenses sync immediately.
     * POST /api/admin/estimates/sync
     */
    @PostMapping("/sync")
    public ResponseEntity<?> runSyncNow() {
        logger.info("Admin triggered estimates -> user_expenses sync");
        try {
            userExpensesEstimatesService.syncAllUsersEstimatesToUserExpenses();
            return ResponseEntity.ok(Map.of("status", "success", "message", "sync completed"));
        } catch (Exception e) {
            logger.error("Error while running estimates sync", e);
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}

