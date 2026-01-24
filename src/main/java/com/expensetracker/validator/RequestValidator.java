package com.expensetracker.validator;

import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.InvalidExpenseAmountException;
import com.expensetracker.exception.InvalidExpenseDateException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validator component for request validation.
 * Validates expense requests including amount and date constraints.
 */
@Component
public class RequestValidator {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;
    private static final int MAX_DECIMAL_SCALE = 2;

    /**
     * Validates an expense request for insert operation.
     * Throws appropriate exceptions if validation fails.
     *
     * @param request the expense request to validate
     * @throws BadRequestException if required fields are missing
     * @throws InvalidExpenseAmountException if expense amount is negative or has more than 2 decimal places
     * @throws InvalidExpenseDateException if expense date is outside valid range (2000-2100)
     */
    public void validateInsertRequest(ExpenseRequest request) {
        if (request == null) {
            throw new BadRequestException("Expense request cannot be null");
        }
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new BadRequestException("userId is required");
        }
        if (request.getExpenseName() == null || request.getExpenseName().isBlank()) {
            throw new BadRequestException("expenseName is required");
        }
        if (request.getExpenseAmount() == null) {
            throw new BadRequestException("expenseAmount is required");
        }
        if (request.getUserExpenseCategoryId() == null) {
            throw new BadRequestException("userExpenseCategoryId is required");
        }
        if (request.getExpenseDate() == null) {
            throw new BadRequestException("expenseDate is required");
        }

        // Validate expense amount is non-negative and has valid decimal places
        validateExpenseAmount(request.getExpenseAmount());

        // Validate expense date is within valid range
        validateExpenseDate(request.getExpenseDate());
    }

    /**
     * Validates an expense request for update operation.
     * Only validates fields that are present (not null).
     *
     * @param request the expense request to validate
     * @throws BadRequestException if expensesId is missing
     * @throws InvalidExpenseAmountException if expense amount is negative or has more than 2 decimal places
     * @throws InvalidExpenseDateException if expense date is outside valid range (2000-2100)
     */
    public void validateUpdateRequest(ExpenseRequest request) {
        if (request == null) {
            throw new BadRequestException("Expense request cannot be null");
        }
        if (request.getExpensesId() == null) {
            throw new BadRequestException("expensesId is required for update");
        }

        // Validate expense amount if provided
        if (request.getExpenseAmount() != null) {
            validateExpenseAmount(request.getExpenseAmount());
        }

        // Validate expense date if provided
        if (request.getExpenseDate() != null) {
            validateExpenseDate(request.getExpenseDate());
        }
    }

    /**
     * Validates that expense amount is non-negative and has at most 2 decimal places.
     *
     * @param amount the expense amount to validate
     * @throws InvalidExpenseAmountException if amount is negative or has more than 2 decimal places
     */
    public void validateExpenseAmount(BigDecimal amount) {
        if (amount == null) {
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidExpenseAmountException("Expense amount must be non-negative. Received: " + amount);
        }
        if (amount.scale() > MAX_DECIMAL_SCALE) {
            throw new InvalidExpenseAmountException("Expense amount can have at most " + MAX_DECIMAL_SCALE + " decimal places. Received: " + amount);
        }
    }

    /**
     * Validates that income amount is non-negative and has at most 2 decimal places.
     *
     * @param amount the income amount to validate
     * @throws InvalidExpenseAmountException if amount is negative or has more than 2 decimal places
     */
    public void validateIncomeAmount(BigDecimal amount) {
        if (amount == null) {
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidExpenseAmountException("Income amount must be non-negative. Received: " + amount);
        }
        if (amount.scale() > MAX_DECIMAL_SCALE) {
            throw new InvalidExpenseAmountException("Income amount can have at most " + MAX_DECIMAL_SCALE + " decimal places. Received: " + amount);
        }
    }

    /**
     * Validates that expense date is within the valid range (2000-2100).
     *
     * @param date the expense date to validate
     * @throws InvalidExpenseDateException if date is outside valid range
     */
    public void validateExpenseDate(LocalDate date) {
        if (date != null) {
            int year = date.getYear();
            if (year < MIN_YEAR || year > MAX_YEAR) {
                throw new InvalidExpenseDateException(date);
            }
        }
    }
}
