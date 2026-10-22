package com.social.service;

import io.jsonwebtoken.Claims;

public interface JwtService {

    /**
     * Generates a signed JWT access token for the given user.
     *
     * @param userId unique user id (subject)
     * @param email  user email claim
     * @return signed compact JWT string
     */
    String generateToken(String userId, String email);

    /**
     * Extracts user id (subject) from a valid JWT token.
     *
     * @param token JWT token string
     * @return userId if valid, or null if invalid/expired
     */
    String extractUserId(String token);

    /**
     * Validates whether the given JWT token is valid and not expired.
     *
     * @param token JWT token string
     * @return true if valid signature and within expiration
     */
    boolean validateToken(String token);

    /**
     * Extracts all claims payload from the token.
     *
     * @param token JWT token string
     * @return Claims payload, or null if invalid
     */
    Claims extractAllClaims(String token);
}
