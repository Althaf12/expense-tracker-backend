package com.expensetracker.util;

import java.util.Set;

/**
 * Application-wide constants.
 */
public final class Constants {

    private Constants() {}

    // Allowed page sizes exposed to API and enforced in services
    public static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 20, 50, 100);

}
