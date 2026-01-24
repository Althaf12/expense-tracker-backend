package com.expensetracker.exception;

/**
 * Exception thrown when an expense adjustment is not found.
 */
public class ExpenseAdjustmentNotFoundException extends ResourceNotFoundException {

    public ExpenseAdjustmentNotFoundException(Integer adjustmentId) {
        super("ExpenseAdjustment", "adjustmentId", adjustmentId);
    }

    public ExpenseAdjustmentNotFoundException(String message) {
        super(message);
    }
}
