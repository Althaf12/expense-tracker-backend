package com.expensetracker.exception;
public class ExpenseCategoryNotFoundException extends ResourceNotFoundException {
    public ExpenseCategoryNotFoundException(Integer categoryId) {
        super("Expense Category", "categoryId", categoryId);
    }
    public ExpenseCategoryNotFoundException(String message) {
        super(message);
    }
}
