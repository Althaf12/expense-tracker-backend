package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserExpensesRequest {
    private String userExpenseName;
    private Integer userExpenseCategoryId;
    private BigDecimal amount;
    private String paid;
    private String status;
}
