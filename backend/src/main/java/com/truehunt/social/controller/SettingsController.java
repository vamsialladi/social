package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.PasswordChangeRequest;
import com.truehunt.social.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.getSettings(resolveCaller(userEmail, userId, authentication))));
    }

    @PatchMapping("/theme")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTheme(
            @RequestBody(required = false) Map<String, String> req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        String theme = (req != null && req.containsKey("theme")) ? req.get("theme") : "dark";
        return ResponseEntity.ok(ApiResponse.success(settingsService.updateTheme(theme, resolveCaller(userEmail, userId, authentication))));
    }

    @PatchMapping("/notifications")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateNotificationPreferences(
            @RequestBody Map<String, Object> prefs,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.updateNotificationPreferences(prefs, resolveCaller(userEmail, userId, authentication))));
    }

    @PatchMapping("/privacy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePrivacyPreferences(
            @RequestBody(required = false) Map<String, Object> prefs,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.updatePrivacyPreferences(prefs, resolveCaller(userEmail, userId, authentication))));
    }

    @PatchMapping("/restricted")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRestrictedPreferences(
            @RequestBody(required = false) Map<String, Object> prefs,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.updateRestrictedPreferences(prefs, resolveCaller(userEmail, userId, authentication))));
    }

    @PatchMapping("/media-visibility")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMediaPreferences(
            @RequestBody(required = false) Map<String, Object> prefs,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.updateMediaPreferences(prefs, resolveCaller(userEmail, userId, authentication))));
    }

    @PatchMapping("/engagement")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateEngagementPreferences(
            @RequestBody(required = false) Map<String, Object> prefs,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.updateEngagementPreferences(prefs, resolveCaller(userEmail, userId, authentication))));
    }

    @PatchMapping("/security")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSecurityPreferences(
            @RequestBody(required = false) Map<String, Object> prefs,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.updateSecurityPreferences(prefs, resolveCaller(userEmail, userId, authentication))));
    }

    @GetMapping("/blocked-users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBlockedUsers(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.getBlockedUsers(resolveCaller(userEmail, userId, authentication))));
    }

    @PostMapping("/blocked-users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> blockUser(
            @RequestBody Map<String, Object> req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.blockUser(req, resolveCaller(userEmail, userId, authentication))));
    }

    @DeleteMapping("/blocked-users/{identity:.+}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unblockUserByPath(
            @PathVariable String identity,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.unblockUser(identity, resolveCaller(userEmail, userId, authentication))));
    }

    @DeleteMapping("/blocked-users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unblockUserByQuery(
            @RequestParam String identity,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.unblockUser(identity, resolveCaller(userEmail, userId, authentication))));
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
