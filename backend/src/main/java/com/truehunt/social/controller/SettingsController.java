package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private Map<String, Boolean> notificationPrefs = new HashMap<>(Map.of(
            "connections", true,
            "network-catch-up", true,
            "posting", true,
            "messaging", true,
            "news", true,
            "profile", true,
            "verifications", true
    ));

    private String currentTheme = "light";

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("theme", currentTheme);
        settings.put("notifications", notificationPrefs);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PatchMapping("/theme")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTheme(@RequestBody Map<String, String> req) {
        String theme = req.get("theme");
        if (theme != null) {
            currentTheme = theme;
        }
        return getSettings();
    }

    @PatchMapping("/notifications")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateNotificationPreferences(@RequestBody Map<String, Boolean> req) {
        notificationPrefs.putAll(req);
        return getSettings();
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody Map<String, String> req) {
        String currentPassword = req.get("current-password");
        String newPassword = req.get("new-password");
        String confirmPassword = req.get("confirm-password");

        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("MISMATCH", "New passwords do not match"));
        }

        return ResponseEntity.ok(ApiResponse.success("Password updated successfully"));
    }

    @PostMapping("/delete-account")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteAccount() {
        Map<String, Object> res = new HashMap<>();
        res.put("status", "SCHEDULED");
        res.put("message", "Your account closure request has been submitted to TrueHunt Administration.");
        return ResponseEntity.ok(ApiResponse.success(res));
    }
}
