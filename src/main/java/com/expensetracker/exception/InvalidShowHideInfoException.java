package com.expensetracker.exception;

import com.expensetracker.util.Constants;

/**
 * Exception thrown when an invalid showHideInfo value is provided.
 */
public class InvalidShowHideInfoException extends ValidationException {

    public InvalidShowHideInfoException(String providedValue) {
        super(String.format("Invalid showHideInfo value: '%s'. Allowed values are: %s",
                providedValue, Constants.VALID_SHOW_HIDE_INFO_VALUES));
    }
}
