package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for analytics summary response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsSummary {
    private Double totalExpenses;
    private Double totalIncome;
    private Double netBalance;
    private Integer totalExpenseCount;
    private Integer totalIncomeCount;
    private Map<String, Double> expensesByCategory;
    private Map<String, Double> incomesBySource;
    private Map<String, Double> monthlyExpenseTrend;
    private Map<String, Double> monthlyIncomeTrend;
}
