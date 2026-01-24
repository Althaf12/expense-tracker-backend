package com.expensetracker.exception;

/**
 * Exception thrown when an invalid adjustment status is provided.
 */
public class InvalidAdjustmentStatusException extends ValidationException {

    public InvalidAdjustmentStatusException(String status) {
        super(String.format("Invalid adjustment status: '%s'. Allowed values are: PENDING, COMPLETED, FAILED, CANCELLED", status));
    }
}
