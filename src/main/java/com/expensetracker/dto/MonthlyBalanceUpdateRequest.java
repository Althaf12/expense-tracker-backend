package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Double openingBalance;
    private Double closingBalance;
}
