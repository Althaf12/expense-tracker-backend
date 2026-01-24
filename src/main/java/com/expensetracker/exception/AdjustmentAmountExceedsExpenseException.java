package com.expensetracker.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when adjustment amount exceeds the allowed limit.
 */
public class AdjustmentAmountExceedsExpenseException extends ValidationException {

    public AdjustmentAmountExceedsExpenseException(BigDecimal adjustmentAmount, BigDecimal expenseAmount) {
        super(String.format("Adjustment amount (%.2f) cannot exceed the expense amount (%.2f)",
                adjustmentAmount, expenseAmount));
    }

    public AdjustmentAmountExceedsExpenseException(BigDecimal adjustmentAmount, BigDecimal totalExistingAdjustments,
                                                    BigDecimal expenseAmount) {
        super(String.format("Total adjustments (%.2f existing + %.2f new = %.2f) cannot exceed the expense amount (%.2f)",
                totalExistingAdjustments, adjustmentAmount,
                totalExistingAdjustments.add(adjustmentAmount), expenseAmount));
    }

    public AdjustmentAmountExceedsExpenseException(String message) {
        super(message);
    }
}
