package com.truehunt.controller;

import com.truehunt.config.UserPrincipal;
import com.truehunt.dto.request.BulkReadNotificationsRequest;
import com.truehunt.dto.response.ApiResponse;
import com.truehunt.dto.response.NotificationResponse;
import com.truehunt.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<NotificationResponse> notifications = notificationService.getNotifications(currentUser.getId(), cursor, limit);
        String nextCursor = null;
        if (!notifications.isEmpty() && notifications.size() >= limit) {
            nextCursor = notifications.get(notifications.size() - 1).getCreatedAt().toString();
        }

        return ResponseEntity.ok(ApiResponse.of(notifications, nextCursor));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markOneRead(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        NotificationResponse response = notificationService.markOneAsRead(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PatchMapping("/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markBulkRead(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody(required = false) BulkReadNotificationsRequest request) {

        List<String> ids = request != null ? request.getNotificationIds() : null;
        int count = notificationService.markAllAsRead(currentUser.getId(), ids);
        return ResponseEntity.ok(ApiResponse.of(Collections.singletonMap("markedReadCount", count)));
    }
}
