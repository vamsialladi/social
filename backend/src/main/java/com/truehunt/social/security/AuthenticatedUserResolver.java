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
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }

        String token = authorization.substring(7);
        if (!tokenProvider.validateToken(token)) {
            return null;
        }

        String email = tokenProvider.getEmailFromToken(token);
        return email == null || email.isBlank() ? null : email.trim();
    }

    public String requireEmail(HttpServletRequest request) {
        String email = resolveEmail(request);
        if (email == null) {
            throw new UnauthorizedException("A valid sign-in is required for contact requests.");
        }
        return email;
    }
}
