package com.expensetracker.security;

/**
 * Constants for cookie names used in JWT authentication.
 * Should match the cookie names used by the Auth service.
 */
public final class CookieService {

    private CookieService() {
        // Utility class - prevent instantiation
    }

    /**
     * Name of the HttpOnly cookie that contains the JWT access token.
     * Must match the cookie name set by the Auth service.
     */
    public static final String ACCESS_TOKEN_COOKIE = "access_token";

    /**
     * Name of the HttpOnly cookie that contains the JWT refresh token.
     */
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
}

