package com.social.service;

public interface RateLimiterService {
    void checkRateLimit(String userId);
    void checkRateLimit(String key, int maxRequests, long windowSeconds, String actionDescription);
}

