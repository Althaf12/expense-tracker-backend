package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncomeRequest {
    private String userId;
    private String source;
    private Double amount;
    private String month;
    private Integer year;
    private LocalDate receivedDate;
}
