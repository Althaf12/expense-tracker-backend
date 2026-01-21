package com.expensetracker.admin.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseCategoryRequest {
    private String expenseCategoryName;
}
