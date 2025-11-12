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
    private String username;
    private String expenseName;
    private Double expenseAmount;
    private Integer expenseCategoryId;
    private LocalDate expenseDate;
}