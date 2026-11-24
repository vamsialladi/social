package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.security.AuthenticatedUserResolver;
import com.truehunt.social.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public NotificationController(
            NotificationService notificationService,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.notificationService = notificationService;
        this.authenticatedUserResolver = authenticatedUserResolver;
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

    @PostMapping("/endorsement-requests")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> sendEndorsementRequests(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request
    ) {
        Object rawRecipientIds = requestBody.get("recipientProfileIds");
        if (!(rawRecipientIds instanceof List<?> recipientIds)) {
            throw new BadRequestException("Select at least one connection.");
        }
        List<String> recipients = recipientIds.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.sendEndorsementRequests(
                        authenticatedUserResolver.requireEmail(request),
                        Objects.toString(requestBody.get("senderProfileId"), ""),
                        recipients
                )
        ));
    }

    @GetMapping("/endorsement-requests/cooldowns")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEndorsementRequestCooldowns(
            @RequestParam String senderProfileId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getEndorsementRequestCooldowns(
                        authenticatedUserResolver.requireEmail(request),
                        senderProfileId
                )
        ));
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
