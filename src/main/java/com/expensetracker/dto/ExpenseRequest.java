package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseRequest {
    private Integer expensesId; // optional for updates
    private String userId;
    private String expenseName;
    private Double expenseAmount;
    private Integer userExpenseCategoryId;
    private LocalDate expenseDate;
}