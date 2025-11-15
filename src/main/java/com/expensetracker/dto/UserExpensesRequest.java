package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserExpensesRequest {
    private String userExpenseName;
    private Integer userExpenseCategoryId;
    private Double amount;
    private String paid;
    private String status;
}
