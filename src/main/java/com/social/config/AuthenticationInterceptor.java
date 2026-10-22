package com.social.config;

import com.social.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String userId = request.getHeader(USER_ID_HEADER);

        if (!StringUtils.hasText(userId)) {
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                userId = authHeader.substring(7).trim();
            }
        }

        if (StringUtils.hasText(userId)) {
            UserContext.setCurrentUserId(userId);
        }

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        UserContext.clear();
    }
}
