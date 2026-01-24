package com.expensetracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Integer expensesId;
    private String userId;
    private String expenseName;
    private BigDecimal expenseAmount;
    private String userExpenseCategoryName; // replace id with name

    // Net expense amount after deducting completed adjustments (refunds, cashbacks, reversals)
    private BigDecimal netExpenseAmount;
    private BigDecimal totalAdjustments;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdateTmstp;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate expenseDate;
}
