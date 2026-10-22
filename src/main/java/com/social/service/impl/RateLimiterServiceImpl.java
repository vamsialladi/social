package com.social.service.impl;

import com.social.exception.RateLimitExceededException;
import com.social.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterServiceImpl implements RateLimiterService {

    private final int maxRequests;
    private final long windowSeconds;
    private final ConcurrentHashMap<String, Deque<Instant>> userRequestTimestamps = new ConcurrentHashMap<>();

    public RateLimiterServiceImpl(
            @Value("${app.messaging.rate-limit.max-requests:30}") int maxRequests,
            @Value("${app.messaging.rate-limit.window-seconds:60}") long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    @Override
    public void checkRateLimit(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(windowSeconds);

        userRequestTimestamps.compute(userId, (key, deque) -> {
            if (deque == null) {
                deque = new ArrayDeque<>();
            }

            // Evict expired timestamps
            while (!deque.isEmpty() && deque.peekFirst().isBefore(windowStart)) {
                deque.pollFirst();
            }

            if (deque.size() >= maxRequests) {
                Instant oldest = deque.peekFirst();
                long secondsToWait = oldest != null 
                        ? Math.max(1, windowSeconds - (now.getEpochSecond() - oldest.getEpochSecond())) 
                        : windowSeconds;
                throw new RateLimitExceededException(
                        "Rate limit exceeded. You can send at most " + maxRequests + " messages per " + windowSeconds + " seconds.",
                        secondsToWait
                );
            }

            deque.addLast(now);
            return deque;
        });
    }
}
