package com.truehunt.controller;

import com.truehunt.config.UserPrincipal;
import com.truehunt.dto.request.LoginRequest;
import com.truehunt.dto.request.RegisterRequest;
import com.truehunt.dto.response.ApiResponse;
import com.truehunt.dto.response.AuthResponse;
import com.truehunt.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.register(request);
        setTokenCookie(response, authResponse.getToken(), 86400);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(authResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.login(request);
        setTokenCookie(response, authResponse.getToken(), 86400);

        return ResponseEntity.ok(ApiResponse.of(authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(HttpServletResponse response) {
        setTokenCookie(response, "", 0);
        return ResponseEntity.ok(ApiResponse.of(Collections.singletonMap("message", "Logged out successfully.")));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getMe(@AuthenticationPrincipal UserPrincipal currentUser) {
        AuthResponse me = authService.getMe(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.of(me));
    }

    private void setTokenCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setSecure(false); // set to true in production HTTPS
        response.addCookie(cookie);
    }
}
