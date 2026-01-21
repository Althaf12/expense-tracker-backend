package com.expensetracker.exception;
public class ExpenseNotFoundException extends ResourceNotFoundException {
    public ExpenseNotFoundException(Integer expenseId) {
        super("Expense", "expenseId", expenseId);
    }
    public ExpenseNotFoundException(String message) {
        super(message);
    }
}
