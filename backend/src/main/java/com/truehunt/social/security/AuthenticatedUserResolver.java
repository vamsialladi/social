package com.truehunt.social.security;

import com.truehunt.social.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserResolver {

    private final JwtTokenProvider tokenProvider;

    public AuthenticatedUserResolver(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public String resolveEmail(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            if (tokenProvider.validateToken(token)) {
                String email = tokenProvider.getEmailFromToken(token);
                if (email != null && !email.isBlank()) {
                    return email.trim();
                }
            }
        }

        String userEmail = request.getHeader("X-User-Email");
        if (userEmail != null && !userEmail.isBlank() && !"me".equalsIgnoreCase(userEmail.trim())) {
            return userEmail.trim();
        }

        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isBlank() && !"me".equalsIgnoreCase(userId.trim())) {
            return userId.trim();
        }

        return null;
    }

    public String requireEmail(HttpServletRequest request) {
        String email = resolveEmail(request);
        if (email == null) {
            throw new UnauthorizedException("A valid sign-in is required for contact requests.");
        }
        return email;
    }

    public String requireJwtEmail(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            if (tokenProvider.validateToken(token)) {
                String email = tokenProvider.getEmailFromToken(token);
                if (email != null && !email.isBlank()) {
                    return email.trim();
                }
            }
        }
        throw new UnauthorizedException("A valid sign-in is required for messaging.");
    }
}
