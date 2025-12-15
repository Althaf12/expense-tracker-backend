package com.expensetracker.util;

import java.util.List;

public class Constants {
    public static final String RESET_URL = "https://expensetracker.eternivity.com/reset-password?token=%s";

    // Valid currency codes used for request validation
    public static final List<String> VALID_CURRENCY_CODES = List.of("INR", "USD", "EUR", "GBP", "AUD", "CAD", "SGD", "AED", "JPY", "CNY");
}
