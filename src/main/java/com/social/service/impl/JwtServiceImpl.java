package com.social.service.impl;

import com.social.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtServiceImpl.class);

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtServiceImpl(
            @Value("${app.auth.jwt.secret:c29jaWFsLW1lZGlhLXNlY3JldC1rZXktZm9yLWF1dGhlbnRpY2F0aW9uLXN5c3RlbS0yMDI2LXN1cGVyLXNlY3VyZQ==}") String secretString,
            @Value("${app.auth.jwt.expiration-seconds:86400}") long expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretString);
            if (keyBytes.length < 32) {
                keyBytes = secretString.getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            keyBytes = secretString.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String generateToken(String userId, String email) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    @Override
    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    @Override
    public boolean validateToken(String token) {
        return extractAllClaims(token) != null;
    }

    @Override
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }
}
