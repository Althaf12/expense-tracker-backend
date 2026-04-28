package com.expensetracker.scheduler;

import com.expensetracker.service.UserExpensesEstimatesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that runs at the start of each month and syncs the
 * UserExpensesEstimates table into the UserExpenses table for all users.
 *
 * The actual sync logic lives in {@link UserExpensesEstimatesService#syncAllUsersEstimatesToUserExpenses()}.
 */
@Component
public class EstimatesScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EstimatesScheduler.class);

    private final UserExpensesEstimatesService userExpensesEstimatesService;

    public EstimatesScheduler(UserExpensesEstimatesService userExpensesEstimatesService) {
        this.userExpensesEstimatesService = userExpensesEstimatesService;
    }

    /**
     * Runs at 00:01 on the 1st day of every month (one minute after midnight,
     * to avoid any race condition with other month-start schedulers).
     * Cron: second  minute  hour  day-of-month  month  day-of-week
     */
    @Scheduled(cron = "0 1 0 1 * ?")
    public void runMonthlyEstimatesSync() {
        logger.info("Starting monthly estimates → user_expenses sync");
        try {
            userExpensesEstimatesService.syncAllUsersEstimatesToUserExpenses();
        } catch (Exception e) {
            logger.error("Error during monthly estimates sync", e);
        }
    }
}

