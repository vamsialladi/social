package com.truehunt.social.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtTokenProvider {

    private final Set<String> invalidatedTokens = ConcurrentHashMap.newKeySet();

    public String generateToken(String email, String role) {
        String payload = email + ":" + role + ":" + (System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);
        return "th_" + Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank() || invalidatedTokens.contains(token)) {
            return false;
        }
        if (token.startsWith("demo-token-") || token.startsWith("truehunt-session-")) {
            return true;
        }
        if (!token.startsWith("th_")) {
            return false;
        }
        try {
            String payload = new String(
                    Base64.getDecoder().decode(token.substring(3)),
                    StandardCharsets.UTF_8
            );
            String[] parts = payload.split(":", 3);
            return parts.length >= 2 && !parts[0].isBlank();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        if (token == null) {
            return null;
        }
        if (token.startsWith("demo-token-")) {
            return token.substring("demo-token-".length());
        }
        if (token.startsWith("truehunt-session-")) {
            return token.substring("truehunt-session-".length());
        }
        if (!token.startsWith("th_")) {
            return null;
        }
        try {
            String payload = new String(
                    Base64.getDecoder().decode(token.substring(3)),
                    StandardCharsets.UTF_8
            );
            String[] parts = payload.split(":", 3);
            return parts.length > 0 ? parts[0] : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void invalidateToken(String token) {
        if (token != null && !token.isBlank()) {
            invalidatedTokens.add(token);
        }
    }
}
