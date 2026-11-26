package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private String resolveCaller(String userEmail, String userId, Authentication authentication) {
        if (userEmail != null && !userEmail.isBlank()) {
            return userEmail.trim();
        }
        if (userId != null && !userId.isBlank()) {
            return userId.trim();
        }
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getNotifications(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotifications(resolveCaller(userEmail, userId, authentication))));
    }

    @GetMapping("/endorsement-requests/cooldowns")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEndorsementRequestCooldowns(
            @RequestParam(value = "senderProfileId", required = false) String senderProfileId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        String resolvedSender = (senderProfileId != null && !senderProfileId.isBlank())
                ? senderProfileId.trim()
                : resolveCaller(userEmail, userId, authentication);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getEndorsementRequestCooldowns(resolvedSender)));
    }

    @PostMapping("/endorsement-requests")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> sendEndorsementRequests(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        String senderProfileId = body != null && body.get("senderProfileId") != null
                ? String.valueOf(body.get("senderProfileId")).trim()
                : resolveCaller(userEmail, userId, authentication);

        List<String> recipientProfileIds = new java.util.ArrayList<>();
        if (body != null && body.get("recipientProfileIds") instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    recipientProfileIds.add(String.valueOf(item).trim());
                }
            }
        }

        String caller = resolveCaller(userEmail, userId, authentication);
        List<Map<String, Object>> created = notificationService.sendEndorsementRequests(senderProfileId, recipientProfileIds, caller);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAsRead(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markAsRead(id)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllAsRead(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markAllAsRead(resolveCaller(userEmail, userId, authentication))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteNotification(@PathVariable String id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Notification deleted successfully", "id", id)));
    }
}
