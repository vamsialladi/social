package com.social.config;

import com.social.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component("currentUserArgumentResolver")
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasUtilAnnotation = parameter.hasParameterAnnotation(com.social.util.CurrentUser.class);
        boolean hasSecurityAnnotation = parameter.hasParameterAnnotation(com.social.security.CurrentUser.class);
        return (hasUtilAnnotation || hasSecurityAnnotation) && parameter.getParameterType().equals(String.class);
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        boolean isRequired = true;
        com.social.util.CurrentUser utilAnnotation = parameter.getParameterAnnotation(com.social.util.CurrentUser.class);
        if (utilAnnotation != null) {
            isRequired = utilAnnotation.required();
        } else {
            com.social.security.CurrentUser secAnnotation = parameter.getParameterAnnotation(com.social.security.CurrentUser.class);
            if (secAnnotation != null) {
                isRequired = secAnnotation.required();
            }
        }

        // 1. Check UserContext
        String currentUserId = com.social.util.UserContext.getCurrentUserId();

        // 2. Check UserContextHolder
        if (!StringUtils.hasText(currentUserId)) {
            currentUserId = UserContextHolder.getUserId();
        }

        // 3. Fallback to request headers / params
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        if (!StringUtils.hasText(currentUserId)) {
            currentUserId = extractUserIdFromRequest(request);
        }

        if (isRequired && !StringUtils.hasText(currentUserId)) {
            throw new UnauthorizedException("User session/authentication is missing. Please sign in.");
        }

        return StringUtils.hasText(currentUserId) ? currentUserId.trim() : null;
    }

    private String extractUserIdFromRequest(HttpServletRequest request) {
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        if (StringUtils.hasText(userIdHeader)) {
            return userIdHeader.trim();
        }
        String altHeader = request.getHeader("userId");
        if (StringUtils.hasText(altHeader)) {
            return altHeader.trim();
        }
        String param = request.getParameter("userId");
        if (StringUtils.hasText(param)) {
            return param.trim();
        }
        return null;
    }
}
