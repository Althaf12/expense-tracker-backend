package com.expensetracker.util;

import java.util.Set;

/**
 * Application-wide constants.
 */
public final class Constants {

    private Constants() {}

    // Allowed page sizes exposed to API and enforced in services
    public static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 20, 50, 100);

    // Maximum records limit for analytics endpoints (to prevent memory issues)
    public static final int MAX_ANALYTICS_RECORDS = 10000;

    // Valid currency codes for user preferences (restricted to requested list)
    public static final Set<String> VALID_CURRENCY_CODES = Set.of(
        "INR", // Indian Rupee
        "USD", // US Dollar
        "EUR", // Euro
        "GBP", // British Pound
        "AUD", // Australian Dollar
        "CAD", // Canadian Dollar
        "SGD", // Singapore Dollar
        "AED", // UAE Dirham
        "JPY", // Japanese Yen
        "CNY"  // Chinese Yuan
    );

    // Valid values for incomeMonth in user preferences: Previous month (P) or Current month (C)
    public static final Set<String> VALID_INCOME_MONTH_VALUES = Set.of("P", "C"); // Previous or Current

    // Valid font sizes for user preferences
    public static final Set<String> VALID_FONT_SIZES = Set.of("S", "M", "L"); // Small, Medium, Large

    // Valid theme values for user preferences (Dark or Light)
    public static final Set<String> VALID_THEMES = Set.of("D", "L"); // Dark or Light

    // Valid adjustment types for expense adjustments
    public static final Set<String> VALID_ADJUSTMENT_TYPES = Set.of("REFUND", "CASHBACK", "REVERSAL");

    // Valid adjustment statuses for expense adjustments
    public static final Set<String> VALID_ADJUSTMENT_STATUSES = Set.of("PENDING", "COMPLETED", "FAILED", "CANCELLED");

    // Completed status for adjustment calculations
    public static final String ADJUSTMENT_STATUS_COMPLETED = "COMPLETED";

}
