package com.expensetracker.exception;
public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(String userId) {
        super("User", "userId", userId);
    }
}
