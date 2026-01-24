package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for updating a monthly balance record.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyBalanceUpdateRequest {
    private String userId;
    private Integer year;
    private Integer month;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
}
