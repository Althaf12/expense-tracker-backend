package com.expensetracker.scheduler;

import com.expensetracker.service.IncomeEstimatesService;
import com.expensetracker.service.UserExpensesEstimatesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that runs at the start of each month and:
 * 1. Syncs UserExpensesEstimates → UserExpenses for all users.
 * 2. Syncs IncomeEstimates → Income for all users, then clears income_estimates.
 */
@Component
public class EstimatesScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EstimatesScheduler.class);

    private final UserExpensesEstimatesService userExpensesEstimatesService;
    private final IncomeEstimatesService incomeEstimatesService;

    public EstimatesScheduler(UserExpensesEstimatesService userExpensesEstimatesService,
                               IncomeEstimatesService incomeEstimatesService) {
        this.userExpensesEstimatesService = userExpensesEstimatesService;
        this.incomeEstimatesService = incomeEstimatesService;
    }

    /**
     * Runs at 00:01 on the 1st day of every month (one minute after midnight,
     * to avoid any race condition with other month-start schedulers).
     * Cron: second  minute  hour  day-of-month  month  day-of-week
     */
    @Scheduled(cron = "0 1 0 1 * ?")
    public void runMonthlyEstimatesSync() {
        // ── 1. Expenses estimates → user_expenses ──────────────────────────
        logger.info("Starting monthly estimates → user_expenses sync");
        try {
            userExpensesEstimatesService.syncAllUsersEstimatesToUserExpenses();
        } catch (Exception e) {
            logger.error("Error during monthly estimates sync", e);
        }

        // ── 2. Income estimates → income (then clear income_estimates) ─────
        logger.info("Starting monthly income estimates → income sync");
        try {
            incomeEstimatesService.syncAllIncomeEstimatesToIncome();
        } catch (Exception e) {
            logger.error("Error during monthly income estimates sync", e);
        }
    }
}
