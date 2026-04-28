package com.expensetracker.exception;

public class IncomeEstimatesNotFoundException extends RuntimeException {

    public IncomeEstimatesNotFoundException(Integer id) {
        super("Income estimate not found with id: " + id);
    }

    public IncomeEstimatesNotFoundException(String message) {
        super(message);
    }
}

