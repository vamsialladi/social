package com.social.service;

import com.social.dto.request.LoginRequest;
import com.social.dto.request.RegisterRequest;
import com.social.dto.response.AuthResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request, String clientIp, HttpServletResponse response);

    AuthResponse login(LoginRequest request, String clientIp, HttpServletResponse response);

    void logout(String token, String currentUserId, HttpServletResponse response);

    AuthResponse getCurrentUser(String currentUserId);
}
