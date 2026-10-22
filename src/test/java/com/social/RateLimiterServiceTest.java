package com.social;

import com.social.exception.RateLimitExceededException;
import com.social.service.impl.RateLimiterServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    @Test
    @DisplayName("Should allow requests within limit and throw RateLimitExceededException when limit exceeded")
    void testRateLimitEnforcement() {
        RateLimiterServiceImpl rateLimiter = new RateLimiterServiceImpl(3, 60);

        // First 3 requests should pass
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit("user-rate-test"));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit("user-rate-test"));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit("user-rate-test"));

        // 4th request must exceed limit
        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkRateLimit("user-rate-test"));

        assertTrue(ex.getMessage().contains("Rate limit exceeded"));
        assertTrue(ex.getRetryAfterSeconds() > 0);

        // Different user should still have their own quota
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit("other-user"));
    }
}
