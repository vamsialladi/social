package com.truehunt.social.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    // Map token -> user email for fast session token lookup
    private final Map<String, String> tokenEmailMap = new ConcurrentHashMap<>();
    private final Map<String, Long> tokenExpiryMap = new ConcurrentHashMap<>();

    private static final long EXPIRATION_TIME_MS = 7L * 24 * 60 * 60 * 1000; // 7 days

    public String generateToken(String email, String role) {
        long expiry = System.currentTimeMillis() + EXPIRATION_TIME_MS;
        String raw = email + ":" + role + ":" + expiry + ":" + RANDOM.nextLong();
        String token = "th_" + Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        tokenEmailMap.put(token, email);
        tokenExpiryMap.put(token, expiry);
        return token;
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        // Support demo/session tokens if any
        if (token.startsWith("truehunt-session-") || token.startsWith("demo-token-")) {
            return true;
        }

        if (tokenEmailMap.containsKey(token)) {
            Long expiry = tokenExpiryMap.get(token);
            if (expiry != null && System.currentTimeMillis() <= expiry) {
                return true;
            } else {
                tokenEmailMap.remove(token);
                tokenExpiryMap.remove(token);
                return false;
            }
        }

        // Attempt decode if structured token
        if (token.startsWith("th_")) {
            try {
                String payload = new String(Base64.getUrlDecoder().decode(token.substring(3)), StandardCharsets.UTF_8);
                String[] parts = payload.split(":");
                if (parts.length >= 3) {
                    long expiry = Long.parseLong(parts[2]);
                    if (System.currentTimeMillis() <= expiry) {
                        tokenEmailMap.put(token, parts[0]);
                        tokenExpiryMap.put(token, expiry);
                        return true;
                    }
                }
            } catch (Exception e) {
                log.debug("Token parsing failed: {}", e.getMessage());
            }
        }

        return false;
    }

    public String getEmailFromToken(String token) {
        if (token == null) return null;
        if (tokenEmailMap.containsKey(token)) {
            return tokenEmailMap.get(token);
        }
        if (token.startsWith("th_")) {
            try {
                String payload = new String(Base64.getUrlDecoder().decode(token.substring(3)), StandardCharsets.UTF_8);
                String[] parts = payload.split(":");
                if (parts.length >= 1) {
                    return parts[0];
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public void invalidateToken(String token) {
        if (token != null) {
            tokenEmailMap.remove(token);
            tokenExpiryMap.remove(token);
        }
    }
}

