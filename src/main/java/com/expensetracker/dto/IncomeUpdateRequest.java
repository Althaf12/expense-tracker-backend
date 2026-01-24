package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncomeUpdateRequest {
    private Integer incomeId;
    private String userId;
    private String source;
    private BigDecimal amount;
    private LocalDate receivedDate;
}
