package com.expensetracker.admin.controller;

import com.expensetracker.service.CurrentBalanceUpdateService;
import com.expensetracker.service.IncomeEstimatesService;
import com.expensetracker.service.UserExpensesEstimatesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin endpoints to manually trigger scheduled tasks for testing purposes.
 *
 * Security: covered by the existing permitAll rule for /api/admin/** in SecurityConfig.
 * Restrict or remove these endpoints in production.
 */
@RestController
@RequestMapping("/api/admin/scheduler")
public class AdminSchedulerController {

    private static final Logger logger = LoggerFactory.getLogger(AdminSchedulerController.class);

    private final UserExpensesEstimatesService userExpensesEstimatesService;
    private final IncomeEstimatesService incomeEstimatesService;
    private final CurrentBalanceUpdateService currentBalanceUpdateService;

    public AdminSchedulerController(UserExpensesEstimatesService userExpensesEstimatesService,
                                    IncomeEstimatesService incomeEstimatesService,
                                    CurrentBalanceUpdateService currentBalanceUpdateService) {
        this.userExpensesEstimatesService = userExpensesEstimatesService;
        this.incomeEstimatesService = incomeEstimatesService;
        this.currentBalanceUpdateService = currentBalanceUpdateService;
    }

    /**
     * Manually trigger the estimates sync (expenses + income + credit cards).
     * Normally runs at 00:01 on the 1st of each month.
     *
     * POST /api/admin/scheduler/run-estimates-sync
     */
    @PostMapping("/run-estimates-sync")
    public ResponseEntity<?> runEstimatesSync() {
        logger.warn("Admin manually triggered estimates sync");

        try {
            // Sync expense estimates
            userExpensesEstimatesService.syncAllUsersEstimatesToUserExpenses();

            // Sync income estimates (but don't delete them)
            int incomeCount = incomeEstimatesService.syncAllIncomeEstimatesToIncome();

            logger.info("Admin estimates sync completed successfully");
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Estimates sync completed",
                    "incomeRecordsCopied", incomeCount
            ));
        } catch (Exception e) {
            logger.error("Error during admin estimates sync", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error during estimates sync: " + e.getMessage()
            ));
        }
    }

    /**
     * Manually trigger the current balance update for all users.
     * Normally runs at 00:05 on the 1st of each month.
     *
     * POST /api/admin/scheduler/run-current-balance-update
     */
    @PostMapping("/run-current-balance-update")
    public ResponseEntity<?> runCurrentBalanceUpdate() {
        logger.warn("Admin manually triggered current balance update");

        try {
            currentBalanceUpdateService.updateCurrentBalanceForAllUsers();

            logger.info("Admin current balance update completed successfully");
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Current balance update completed for all users"
            ));
        } catch (Exception e) {
            logger.error("Error during admin current balance update", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error during current balance update: " + e.getMessage()
            ));
        }
    }
}

