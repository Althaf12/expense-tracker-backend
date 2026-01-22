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

}
