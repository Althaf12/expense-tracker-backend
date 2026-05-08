package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for category-level analytics summary response.
 * Returns consolidated expense totals broken down by category
 * for a given month, year, or date range.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryAnalyticsSummary {

    /** Gross total expenses (before adjustments) */
    private BigDecimal totalExpenses;

    /** Total completed adjustments (refunds/cashbacks/reversals) */
    private BigDecimal totalAdjustments;

    /** Net expenses after deducting all adjustments */
    private BigDecimal netExpenses;

    /** Total number of expense records */
    private Integer totalRecords;

    /**
     * Net expense total per category (after adjustments).
     * Key: category name, Value: net amount
     */
    private Map<String, BigDecimal> categoryTotals;

    /**
     * Gross expense total per category (before adjustments).
     * Key: category name, Value: gross amount
     */
    private Map<String, BigDecimal> categoryGrossTotals;

    /**
     * Total adjustments per category.
     * Key: category name, Value: total adjustment amount
     */
    private Map<String, BigDecimal> categoryAdjustments;

    /**
     * Number of expense records per category.
     * Key: category name, Value: count
     */
    private Map<String, Integer> categoryRecordCounts;
}

