package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.model.NetworkProfileEntity;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.repository.NetworkProfileRepository;
import com.truehunt.social.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

    private final NetworkProfileRepository networkProfileRepository;
    private final NotificationRepository notificationRepository;

    public ConnectionController(NetworkProfileRepository networkProfileRepository, NotificationRepository notificationRepository) {
        this.networkProfileRepository = networkProfileRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSuggestions() {
        List<Map<String, Object>> list = networkProfileRepository.findAll().stream()
                .map(NetworkProfileEntity::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConnectedProfiles() {
        List<Map<String, Object>> list = networkProfileRepository.findAllByConnectedTrue().stream()
                .map(NetworkProfileEntity::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/toggle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleConnection(@RequestBody Map<String, String> req) {
        String profileId = req.get("profileId");
        if (profileId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "profileId is required"));
        }

        Optional<NetworkProfileEntity> opt = networkProfileRepository.findById(profileId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Profile not found"));
        }

        NetworkProfileEntity p = opt.get();
        boolean newStatus = !p.isConnected();
        p.setConnected(newStatus);
        networkProfileRepository.save(p);

        // If newly connected, add a notification
        if (newStatus) {
            notificationRepository.save(new NotificationEntity(
                    "me",
                    "New Connection",
                    "You are now connected with " + p.getName() + " (" + p.getRole() + ")",
                    "connection"
            ));
        }

        return ResponseEntity.ok(ApiResponse.success(p.toDto()));
    }

    @PostMapping("/follow")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleFollow(@RequestBody Map<String, String> req) {
        String profileId = req.get("profileId");
        if (profileId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "profileId is required"));
        }

        Optional<NetworkProfileEntity> opt = networkProfileRepository.findById(profileId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Profile not found"));
        }

        NetworkProfileEntity p = opt.get();
        p.setFollowing(!p.isFollowing());
        networkProfileRepository.save(p);

        return ResponseEntity.ok(ApiResponse.success(p.toDto()));
    }
}
