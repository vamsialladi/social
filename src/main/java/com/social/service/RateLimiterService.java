package com.social.service;

public interface RateLimiterService {
    void checkRateLimit(String userId);
}
