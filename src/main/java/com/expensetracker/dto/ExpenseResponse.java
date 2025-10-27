package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Integer expensesId;
    private String userId;
    private String expenseName;
    private Double expenseAmount;
    private String expenseCategoryName; // replace id with name
    private LocalDateTime lastUpdateTmstp;
    private LocalDate expenseDate;
}

