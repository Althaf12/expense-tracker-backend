package com.expensetracker.exception;

public class UserCreditCardEstimatesNotFoundException extends RuntimeException {
    public UserCreditCardEstimatesNotFoundException(Integer id) {
        super("User credit card estimate not found with id: " + id);
    }
    public UserCreditCardEstimatesNotFoundException(String message) {
        super(message);
    }
}

