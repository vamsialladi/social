package com.social.controller;

import com.social.dto.request.MarkNotificationsReadRequest;
import com.social.dto.response.ApiResponse;
import com.social.dto.response.NotificationResponse;
import com.social.dto.response.NotificationsPageResponse;
import com.social.service.NotificationService;
import com.social.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Module 8: User notifications for connection requests, messages, and events")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List notifications of the signed-in user",
               description = "Returns notifications where recipientId matches the signed-in user, supporting cursor pagination (newest first).")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit,
            @Parameter(hidden = true) @CurrentUser String currentUserId) {
        NotificationsPageResponse response = notificationService.getNotificationsForUser(currentUserId, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(response.getNotifications(), response.getNextCursor()));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a single notification as read",
               description = "Verifies ownership and updates readAt timestamp.")
    public ResponseEntity<ApiResponse<NotificationResponse>> markOneAsRead(
            @PathVariable("id") String notificationId,
            @Parameter(hidden = true) @CurrentUser String currentUserId) {
        NotificationResponse response = notificationService.markOneAsRead(notificationId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/read")
    @Operation(summary = "Mark many notifications as read",
               description = "Marks specified notifications as read, or all unread notifications if no IDs provided.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markManyAsRead(
            @RequestBody(required = false) MarkNotificationsReadRequest request,
            @Parameter(hidden = true) @CurrentUser String currentUserId) {
        int updatedCount = notificationService.markManyAsRead(
                currentUserId,
                request != null ? request.getNotificationIds() : null
        );
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "markedReadCount", updatedCount
        )));
    }
}
