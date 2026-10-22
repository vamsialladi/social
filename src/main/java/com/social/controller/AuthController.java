package com.social.controller;

import com.social.dto.request.LoginRequest;
import com.social.dto.request.RegisterRequest;
import com.social.dto.response.ApiResponse;
import com.social.dto.response.AuthResponse;
import com.social.service.AuthService;
import com.social.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Module 1: User registration, login, logout, and caller identification")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user",
               description = "Creates user and an empty Profile, starts session, and sets secure cookie.")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String clientIp = extractClientIp(httpRequest);
        AuthResponse response = authService.register(request, clientIp, httpResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    @Operation(summary = "Verify credentials and start session",
               description = "Verifies email and password, creates a secure session cookie, and identifies caller on later requests.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String clientIp = extractClientIp(httpRequest);
        AuthResponse response = authService.login(request, clientIp, httpResponse);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "End session",
               description = "Ends current session, invalidates session, and clears session cookie.")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            @Parameter(hidden = true) @CurrentUser(required = false) String currentUserId) {
        String sessionId = extractSessionId(httpRequest);
        authService.logout(sessionId, currentUserId, httpResponse);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Logged out successfully")));
    }

    @GetMapping("/me")
    @Operation(summary = "Get signed-in user and permitted profile fields",
               description = "Returns current authenticated account and allowed profile fields based on session cookie.")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(
            @Parameter(hidden = true) @CurrentUser String currentUserId) {
        AuthResponse response = authService.getCurrentUser(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : "127.0.0.1";
    }

    private String extractSessionId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authHeader.substring(7).trim();
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("session_token".equals(cookie.getName()) || "auth_token".equals(cookie.getName())) {
                    if (StringUtils.hasText(cookie.getValue())) {
                        return cookie.getValue().trim();
                    }
                }
            }
        }
        String sessionHeader = request.getHeader("X-Session-Id");
        if (StringUtils.hasText(sessionHeader)) {
            return sessionHeader.trim();
        }
        return null;
    }
}
