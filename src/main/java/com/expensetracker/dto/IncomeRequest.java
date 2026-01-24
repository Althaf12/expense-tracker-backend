package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncomeRequest {
    private String userId;
    private String source;
    private BigDecimal amount;
    private String month;
    private Integer year;
    private LocalDate receivedDate;
}
