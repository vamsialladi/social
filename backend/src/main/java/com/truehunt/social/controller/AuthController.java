package com.truehunt.social.controller;

import com.truehunt.social.dto.*;
import com.truehunt.social.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<OtpResponse>> sendOtp(@RequestBody(required = false) OtpRequest req) {
        OtpResponse resp = authService.sendOtp(req);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<OtpResponse>> verifyOtp(@RequestBody(required = false) OtpRequest req) {
        OtpResponse resp = authService.verifyOtp(req);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest credentials,
            HttpServletRequest request
    ) {
        AuthResponse resp = authService.login(credentials, resolveClientIp(request));
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse resp = authService.register(req);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.requestPasswordReset(req)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.resetPassword(req)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMe(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            org.springframework.security.core.Authentication authentication
    ) {
        String identifier = (userEmail != null && !userEmail.isBlank()) ? userEmail : userId;
        if ((identifier == null || identifier.isBlank())
                && authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            identifier = authentication.getName();
        }
        Map<String, Object> user = authService.getCurrentUser(identifier);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Logged out successfully")));
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
