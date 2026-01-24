package com.expensetracker.exception;

/**
 * Exception thrown when an invalid adjustment type is provided.
 */
public class InvalidAdjustmentTypeException extends ValidationException {

    public InvalidAdjustmentTypeException(String type) {
        super(String.format("Invalid adjustment type: '%s'. Allowed values are: REFUND, CASHBACK, REVERSAL", type));
    }
}
