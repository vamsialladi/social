package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getNotifications() {
        List<Map<String, Object>> dtos = notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(NotificationEntity::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAsRead(@PathVariable Long id) {
        Optional<NotificationEntity> opt = notificationRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Notification not found"));
        }

        NotificationEntity n = opt.get();
        n.setRead(true);
        NotificationEntity saved = notificationRepository.save(n);
        return ResponseEntity.ok(ApiResponse.success(saved.toDto()));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<String>> markAllAsRead() {
        List<NotificationEntity> all = notificationRepository.findAll();
        for (NotificationEntity n : all) {
            n.setRead(true);
        }
        notificationRepository.saveAll(all);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteNotification(@PathVariable Long id) {
        if (notificationRepository.existsById(id)) {
            notificationRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully"));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Notification not found"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createEndorsement(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "New Endorsement");
        String message = body.getOrDefault("message", "A new endorsement was added to your profile.");
        String type = body.getOrDefault("type", "endorsement");

        NotificationEntity n = new NotificationEntity("me", title, message, type);
        NotificationEntity saved = notificationRepository.save(n);

        return ResponseEntity.ok(ApiResponse.success(saved.toDto()));
    }
}
