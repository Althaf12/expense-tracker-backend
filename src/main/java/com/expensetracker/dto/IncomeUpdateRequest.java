package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncomeUpdateRequest {
    private Integer incomeId;
    private String username;
    private String source;
    private Double amount;
    private LocalDate receivedDate;
}

