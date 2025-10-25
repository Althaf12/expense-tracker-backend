package com.expensetracker.validator;

import com.expensetracker.dto.ExpenseRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestValidator {
    public boolean validateInsertRequest(ExpenseRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId().isBlank()) {
            return false;
        }
        if (request.getExpenseName() == null || request.getExpenseName().isBlank()) {
            return false;
        }
        if (request.getExpenseAmount() == null) {
            return false;
        }
        if (request.getExpenseCategoryId() == null) {
            return false;
        }
        if (request.getExpenseDate() == null) {
            return false;
        }
        return true;
    }
}
