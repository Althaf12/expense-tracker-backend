package com.expensetracker.exception;

/**
 * Thrown when a bank statement PDF cannot be parsed or processed.
 */
public class BankStatementProcessingException extends RuntimeException {

    public BankStatementProcessingException(String message) {
        super(message);
    }

    public BankStatementProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

