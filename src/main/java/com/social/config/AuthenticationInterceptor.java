package com.social.config;

import com.social.entity.User;
import com.social.entity.UserSession;
import com.social.entity.UserStatus;
import com.social.exception.ForbiddenException;
import com.social.repository.UserRepository;
import com.social.repository.UserSessionRepository;
import com.social.service.JwtService;
import com.social.util.UserContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String SESSION_ID_HEADER = "X-Session-Id";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String SESSION_COOKIE_NAME = "session_token";
    public static final String AUTH_COOKIE_NAME = "auth_token";

    private final UserSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthenticationInterceptor(UserSessionRepository sessionRepository,
                                    UserRepository userRepository,
                                    JwtService jwtService) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String token = extractAuthToken(request);
        String resolvedUserId = null;

        if (StringUtils.hasText(token)) {
            // 1. Try decoding as JWT token
            String jwtUserId = jwtService.extractUserId(token);
            if (StringUtils.hasText(jwtUserId)) {
                resolvedUserId = jwtUserId;
            } else {
                // 2. Try DB-backed session
                Optional<UserSession> sessionOpt = sessionRepository.findByToken(token);
                if (sessionOpt.isPresent()) {
                    UserSession session = sessionOpt.get();
                    if (!session.isExpired()) {
                        resolvedUserId = session.getUserId();
                    }
                } else if (userRepository.existsById(token)) {
                    // Backward-compatible fallback for direct user ID in session token
                    resolvedUserId = token;
                }
            }
        }

        // Fallback to X-User-Id or userId header if not resolved via JWT or session cookie
        if (!StringUtils.hasText(resolvedUserId)) {
            resolvedUserId = request.getHeader(USER_ID_HEADER);
            if (!StringUtils.hasText(resolvedUserId)) {
                resolvedUserId = request.getHeader("userId");
            }
        }

        if (StringUtils.hasText(resolvedUserId)) {
            // Verify user status if user exists in database
            Optional<User> userOpt = userRepository.findById(resolvedUserId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getStatus() == UserStatus.SUSPENDED) {
                    throw new ForbiddenException("Account is suspended");
                }
                if (user.getStatus() == UserStatus.DELETED) {
                    throw new ForbiddenException("Account has been deleted");
                }
            }

            UserContext.setCurrentUserId(resolvedUserId);
            UserContextHolder.setUserId(resolvedUserId);
        }

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        UserContext.clear();
        UserContextHolder.clear();
    }

    private String extractAuthToken(HttpServletRequest request) {
        // 1. Check Authorization header (Bearer token)
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }

        // 2. Check cookies for session_token or auth_token
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName()) || AUTH_COOKIE_NAME.equals(cookie.getName())) {
                    if (StringUtils.hasText(cookie.getValue())) {
                        return cookie.getValue().trim();
                    }
                }
            }
        }

        // 3. Check X-Session-Id header
        String sessionHeader = request.getHeader(SESSION_ID_HEADER);
        if (StringUtils.hasText(sessionHeader)) {
            return sessionHeader.trim();
        }

        return null;
    }
}
