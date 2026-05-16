package com.expensetracker.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Unified request DTO for all paginated expense listing endpoints.
 *
 * <p>Endpoint-specific required fields:
 * <ul>
 *   <li>{@code /api/expense/all}   — userId</li>
 *   <li>{@code /api/expense/range} — userId, start, end</li>
 *   <li>{@code /api/expense/month} — userId, year, month</li>
 *   <li>{@code /api/expense/year}  — userId, year</li>
 * </ul>
 *
 * <p>All sorting and filter fields are optional.
 * When absent the endpoint behaves exactly as it did before.
 */
@Data
@NoArgsConstructor
public class ExpensePageRequest {

    // ── Required (varies per endpoint) ───────────────────────────────────
    private String userId;

    /** ISO date "YYYY-MM-DD" — used by /range */
    private String start;
    /** ISO date "YYYY-MM-DD" — used by /range */
    private String end;

    /** 1–12 — used by /month */
    private Integer month;
    /** e.g. 2026 — used by /year and /month */
    private Integer year;

    // ── Pagination ────────────────────────────────────────────────────────
    /** Zero-based page index. Default 0. */
    private Integer page;
    /** Page size. Allowed: 10, 20, 50, 100. Default 10. */
    private Integer size;

    // ── Sorting (optional) ────────────────────────────────────────────────
    /**
     * Column to sort by. Accepted values (case-insensitive):
     * {@code expenseName}, {@code expenseAmount}, {@code expenseDate}, {@code categoryName}.
     * Omit or send null/blank to use the default (newest date first).
     */
    private String sortBy;
    /**
     * Sort direction: {@code ASC} or {@code DESC}.
     * Defaults to {@code DESC} when omitted.
     */
    private String sortDir;

    // ── Filters (all optional, combined with AND logic) ───────────────────

    /** Case-insensitive "contains" match on expense name. */
    private String filterName;

    /** Case-insensitive "contains" match on category name. */
    private String filterCategory;

    /**
     * Amount comparison operator: {@code LT} (less than), {@code EQ} (equals),
     * {@code GT} (greater than). Must be paired with {@code filterAmountValue}.
     */
    private String filterAmountOp;

    /** Numeric threshold for the amount filter. */
    private BigDecimal filterAmountValue;

    /**
     * Date sub-filter granularity: {@code Date}, {@code Month}, or {@code Year}.
     * Must be paired with {@code filterDateValue}.
     */
    private String filterDateType;

    /**
     * Value for the date sub-filter:
     * <ul>
     *   <li>{@code filterDateType=Date}  → "YYYY-MM-DD", e.g. {@code "2026-04-15"}</li>
     *   <li>{@code filterDateType=Month} → "1"–"12",    e.g. {@code "4"} for April</li>
     *   <li>{@code filterDateType=Year}  → "YYYY",       e.g. {@code "2026"}</li>
     * </ul>
     */
    private String filterDateValue;
}

