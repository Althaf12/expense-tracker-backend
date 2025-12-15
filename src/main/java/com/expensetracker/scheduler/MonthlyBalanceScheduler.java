package com.expensetracker.scheduler;

import com.expensetracker.controller.MonthlyBalanceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class MonthlyBalanceScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MonthlyBalanceScheduler.class);

    private final MonthlyBalanceController monthlyBalanceController;

    public MonthlyBalanceScheduler(MonthlyBalanceController monthlyBalanceController) {
        this.monthlyBalanceController = monthlyBalanceController;
    }

    // Runs at 00:00 on the 1st day of every month (server time).
    // Cron expression: "0 0 0 1 * ?"
    // Explanation: second minute hour day-of-month month day-of-week
    @Scheduled(cron = "0 0 0 1 * ?")
    public void runMonthlySnapshot() {
        // snapshot for the month that just finished (previous month)
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        logger.info("Starting monthly balance generation for {}-{}", previousMonth.getYear(), previousMonth.getMonthValue());

        try {
            var response = monthlyBalanceController.generateForMonth(previousMonth.getYear(), previousMonth.getMonthValue());
            logger.info("Monthly balance generation invoked via controller, response: {} - {}", response.getStatusCode(), response.getBody());
        } catch (Exception e) {
            logger.error("Error invoking monthly balance generation via controller for {}", previousMonth, e);
        }
    }
}
