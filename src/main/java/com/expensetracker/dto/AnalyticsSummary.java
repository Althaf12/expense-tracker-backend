package com.expensetracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * DTO for analytics summary response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    /** Actual income start date used (may differ from expense range when preference is P) */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate incomeRangeStart;

    /** Actual income end date used (may differ from expense range when preference is P) */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate incomeRangeEnd;

    /** Income month preference: C = current month, P = previous month */
    private String incomeMonthPreference;
}
