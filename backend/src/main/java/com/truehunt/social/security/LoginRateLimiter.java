package com.truehunt.social.security;

import com.truehunt.social.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory sliding-window rate limiter for failed login attempts.
 */
@Component
public class LoginRateLimiter {

    private final int maxAttempts;
    private final long windowSeconds;
    private final Map<String, Deque<Instant>> failedAttempts = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${app.auth.login-rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${app.auth.login-rate-limit.window-seconds:900}") long windowSeconds
    ) {
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
    }

    public void checkAllowed(String email, String clientIp) {
        String key = buildKey(email, clientIp);
        if (key == null) {
            return;
        }
        Deque<Instant> timestamps = failedAttempts.get(key);
        if (timestamps == null) {
            return;
        }
        pruneExpired(timestamps, Instant.now());
        if (timestamps.size() >= maxAttempts) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "TOO_MANY_REQUESTS",
                    "Too many failed login attempts. Please try again later."
            );
        }
    }

    public void recordFailure(String email, String clientIp) {
        String key = buildKey(email, clientIp);
        if (key == null) {
            return;
        }
        Instant now = Instant.now();
        Deque<Instant> timestamps = failedAttempts.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());
        pruneExpired(timestamps, now);
        timestamps.addLast(now);
    }

    public void clearFailures(String email, String clientIp) {
        String key = buildKey(email, clientIp);
        if (key != null) {
            failedAttempts.remove(key);
        }
    }

    public void clearFailuresForEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        String prefix = email.trim().toLowerCase(Locale.ROOT) + "|";
        failedAttempts.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public void resetAll() {
        failedAttempts.clear();
    }

    private void pruneExpired(Deque<Instant> timestamps, Instant now) {
        Instant cutoff = now.minusSeconds(windowSeconds);
        while (!timestamps.isEmpty()) {
            Instant head = timestamps.peekFirst();
            if (head != null && head.isBefore(cutoff)) {
                timestamps.pollFirst();
            } else {
                break;
            }
        }
    }

    private String buildKey(String email, String clientIp) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String normalizedIp = (clientIp == null || clientIp.isBlank()) ? "local" : clientIp.trim();
        if (normalizedEmail.isEmpty() && normalizedIp.equals("local")) {
            return null;
        }
        return normalizedEmail + "|" + normalizedIp;
    }
}
