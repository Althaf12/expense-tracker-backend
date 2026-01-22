package com.expensetracker.exception;

/**
 * Exception thrown when a monthly balance record is not found.
 */
public class MonthlyBalanceNotFoundException extends RuntimeException {

    public MonthlyBalanceNotFoundException(String message) {
        super(message);
    }

    public MonthlyBalanceNotFoundException(String userId, int year, int month) {
        super(String.format("Monthly balance not found for user '%s' for %d-%02d", userId, year, month));
    }
}
