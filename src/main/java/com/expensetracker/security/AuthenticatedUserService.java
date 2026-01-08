package com.expensetracker.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility class to access the currently authenticated user's information.
 */
@Component
public class AuthenticatedUserService {

    /**
     * Gets the user ID of the currently authenticated user.
     * This user ID matches the user_id column in the database tables.
     *
     * @return the authenticated user's ID, or null if not authenticated
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof String) {
                return (String) principal;
            }
            return principal.toString();
        }
        return null;
    }

    /**
     * Checks if the current user is authenticated.
     *
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * Validates that the requested user ID matches the authenticated user.
     * This prevents users from accessing other users' data.
     *
     * @param requestedUserId the user ID being requested
     * @return true if the authenticated user matches the requested user ID
     */
    public boolean isCurrentUser(String requestedUserId) {
        String currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(requestedUserId);
    }
}

