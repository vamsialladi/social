package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.PasswordChangeRequest;
import com.truehunt.social.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    private String resolveCaller(String userEmail, String userId, Authentication authentication) {
        if (userEmail != null && !userEmail.isBlank()) {
            return userEmail;
        }
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(settingsService.getSettings()));
    }

    @PatchMapping("/theme")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTheme(@RequestBody(required = false) Map<String, String> req) {
        String theme = (req != null && req.containsKey("theme")) ? req.get("theme") : "dark";
        return ResponseEntity.ok(ApiResponse.success(settingsService.updateTheme(theme)));
    }

    @PatchMapping("/notifications")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateNotificationPreferences(@RequestBody Map<String, Object> prefs) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.updateNotificationPreferences(prefs)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changePassword(
            @Valid @RequestBody PasswordChangeRequest req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.changePassword(req, resolveCaller(userEmail, userId, authentication))));
    }

    @PostMapping("/delete-account")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteAccount(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.deleteAccount(resolveCaller(userEmail, userId, authentication))));
    }
}
