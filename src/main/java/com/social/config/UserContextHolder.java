package com.social.config;

import com.social.exception.UnauthorizedException;

public final class UserContextHolder {

    private static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void setUserId(String userId) {
        CURRENT_USER.set(userId);
    }

    public static String getUserId() {
        return CURRENT_USER.get();
    }

    public static String requireUserId() {
        String userId = CURRENT_USER.get();
        if (userId == null || userId.trim().isEmpty()) {
            throw new UnauthorizedException("Authentication required: Missing or empty X-User-Id header");
        }
        return userId;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
