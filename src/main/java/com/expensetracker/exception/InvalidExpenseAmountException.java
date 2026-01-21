package com.expensetracker.exception;
public class InvalidExpenseAmountException extends ValidationException {
    public InvalidExpenseAmountException(Double amount) {
        super(String.format("Expense amount must be non-negative. Received: %s", amount));
    }
    public InvalidExpenseAmountException(String message) {
        super(message);
    }
}
