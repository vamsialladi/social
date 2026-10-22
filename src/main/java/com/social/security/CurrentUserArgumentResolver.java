package com.social.security;

import com.social.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
                parameter.getParameterType().equals(String.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
        boolean isRequired = annotation != null && annotation.required();

        String userId = extractUserId(request);

        if (isRequired && !StringUtils.hasText(userId)) {
            throw new UnauthorizedException("Authentication required. Please provide a valid '" 
                    + USER_ID_HEADER + "' header or Bearer token.");
        }

        return StringUtils.hasText(userId) ? userId.trim() : null;
    }

    private String extractUserId(HttpServletRequest request) {
        // 1. Check UserContextHolder or UserContext set by AuthenticationInterceptor
        String contextUser = com.social.config.UserContextHolder.getUserId();
        if (StringUtils.hasText(contextUser)) {
            return contextUser;
        }
        String legacyUserContext = com.social.util.UserContext.getCurrentUserId();
        if (StringUtils.hasText(legacyUserContext)) {
            return legacyUserContext;
        }

        // 2. Check X-User-Id header
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        if (StringUtils.hasText(userIdHeader)) {
            return userIdHeader;
        }

        // 2. Check Authorization Bearer header
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 3. Check query param for local dev/testing flexibility
        String param = request.getParameter("userId");
        if (StringUtils.hasText(param)) {
            return param;
        }

        return null;
    }
}
