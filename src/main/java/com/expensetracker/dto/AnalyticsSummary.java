package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for analytics summary response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsSummary {
    private BigDecimal totalExpenses;
    private BigDecimal totalIncome;
    private BigDecimal netBalance;
    private BigDecimal totalAdjustments; // Total completed adjustments (refunds/cashbacks/reversals)
    private BigDecimal netExpenses; // Total expenses after deducting adjustments
    private Integer totalExpenseCount;
    private Integer totalIncomeCount;
    private Map<String, BigDecimal> expensesByCategory;
    private Map<String, BigDecimal> incomesBySource;
    private Map<String, BigDecimal> monthlyExpenseTrend;
    private Map<String, BigDecimal> monthlyIncomeTrend;
}
