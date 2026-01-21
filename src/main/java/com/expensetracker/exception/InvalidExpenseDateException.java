package com.expensetracker.exception;
import java.time.LocalDate;
public class InvalidExpenseDateException extends ValidationException {
    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;
    public InvalidExpenseDateException(LocalDate date) {
        super(String.format("Expense date must be between year %d and %d. Received: %s", MIN_YEAR, MAX_YEAR, date));
    }
    public InvalidExpenseDateException(String message) {
        super(message);
    }
}
