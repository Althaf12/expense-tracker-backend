package com.expensetracker.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Unified request DTO for all paginated income listing endpoints.
 *
 * <p>Endpoint-specific required fields:
 * <ul>
 *   <li>{@code /api/income/range} — userId, fromMonth, fromYear, toMonth, toYear</li>
 *   <li>{@code /api/income/month} — userId, year, month</li>
 *   <li>{@code /api/income/year}  — userId, year</li>
 * </ul>
 *
 * <p>All sorting and filter fields are optional.
 * When absent the endpoint behaves exactly as it did before.
 */
@Data
@NoArgsConstructor
public class IncomePageRequest {

    // ── Required (varies per endpoint) ───────────────────────────────────
    private String userId;

    /** 1–12, used by /range */
    private String fromMonth;
    /** e.g. "2026", used by /range */
    private String fromYear;
    /** 1–12, used by /range */
    private String toMonth;
    /** e.g. "2026", used by /range */
    private String toYear;

    /** 1–12 — used by /month */
    private Integer month;
    /** e.g. 2026 — used by /month */
    private Integer year;

    // ── Pagination ────────────────────────────────────────────────────────
    /** Zero-based page index. Default 0. */
    private Integer page;
    /** Page size. Allowed: 10, 20, 50, 100. Default 10. */
    private Integer size;

    // ── Sorting (optional) ────────────────────────────────────────────────
    /**
     * Column to sort by. Accepted values (case-insensitive):
     * {@code source}, {@code amount}, {@code receivedDate}.
     * Omit or send null/blank to use the default (newest receivedDate first).
     */
    private String sortBy;
    /**
     * Sort direction: {@code ASC} or {@code DESC}.
     * Defaults to {@code DESC} when omitted.
     */
    private String sortDir;

    // ── Filters (all optional, combined with AND logic) ───────────────────

    /** Case-insensitive "contains" match on income source. */
    private String filterSource;

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

