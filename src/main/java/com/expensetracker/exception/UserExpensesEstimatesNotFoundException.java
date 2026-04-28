package com.expensetracker.exception;

public class UserExpensesEstimatesNotFoundException extends RuntimeException {
    public UserExpensesEstimatesNotFoundException(Integer id) {
        super("User expenses estimate not found with id: " + id);
    }
    public UserExpensesEstimatesNotFoundException(String message) {
        super(message);
    }
}

