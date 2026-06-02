package com.expensetracker.scheduler;

import com.expensetracker.service.CurrentBalanceUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that runs after the monthly balance snapshot and updates the
 * current_closing_balance for all users based on their income month preference.
 *
 * Runs at 00:05 on the 1st day of every month (5 minutes after the monthly
 * balance scheduler at 00:00) to ensure the monthly balance has been created.
 */
@Component
public class CurrentBalanceScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CurrentBalanceScheduler.class);

    private final CurrentBalanceUpdateService currentBalanceUpdateService;

    public CurrentBalanceScheduler(CurrentBalanceUpdateService currentBalanceUpdateService) {
        this.currentBalanceUpdateService = currentBalanceUpdateService;
    }

    /**
     * Runs at 00:05 on the 1st day of every month (server time).
     * Cron expression: "0 5 0 1 * ?"
     * Explanation: second minute hour day-of-month month day-of-week
     *
     * This runs 5 minutes after the monthly balance scheduler to ensure
     * the previous month's closing balance is available in the monthly_balance table.
     */
    @Scheduled(cron = "0 5 0 1 * ?")
    public void runMonthlyCurrentBalanceUpdate() {
        logger.info("Starting monthly current balance update for all users");

        try {
            currentBalanceUpdateService.updateCurrentBalanceForAllUsers();
            logger.info("Monthly current balance update completed successfully");
        } catch (Exception e) {
            logger.error("Error during monthly current balance update", e);
        }
    }
}

