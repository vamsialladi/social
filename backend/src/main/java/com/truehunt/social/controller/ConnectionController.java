package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.ConnectionRequestDto;
import com.truehunt.social.service.ConnectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

    private final ConnectionService connectionService;

    public ConnectionController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    private String resolveUserIdentifier(String userEmail, String userId) {
        if (userId != null && !userId.isBlank()) {
            return userId.trim();
        }
        return userEmail;
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSuggestions(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(connectionService.getSuggestions(resolveUserIdentifier(userEmail, userId))));
    }

    @GetMapping("/invitations")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getInvitations(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(connectionService.getInvitations(resolveUserIdentifier(userEmail, userId))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConnectedProfiles(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(connectionService.getConnectedProfiles(resolveUserIdentifier(userEmail, userId))));
    }

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendConnectionRequest(
            @RequestBody(required = false) Map<String, String> req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        String profileId = req != null ? (req.get("profileId") != null ? req.get("profileId") : req.get("talentId")) : null;
        String message = req != null ? req.get("message") : null;
        return ResponseEntity.ok(ApiResponse.success(connectionService.sendConnectionRequest(profileId, message, resolveUserIdentifier(userEmail, userId))));
    }

    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<Map<String, Object>>> acceptConnection(
            @RequestBody(required = false) Map<String, String> req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        String profileId = req != null ? (req.get("profileId") != null ? req.get("profileId") : req.get("senderId")) : null;
        return ResponseEntity.ok(ApiResponse.success(connectionService.acceptConnection(profileId, resolveUserIdentifier(userEmail, userId))));
    }

    @PostMapping("/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectConnection(
            @RequestBody(required = false) Map<String, String> req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        String profileId = req != null ? (req.get("profileId") != null ? req.get("profileId") : req.get("senderId")) : null;
        return ResponseEntity.ok(ApiResponse.success(connectionService.rejectConnection(profileId, resolveUserIdentifier(userEmail, userId))));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelConnection(
            @RequestBody(required = false) Map<String, String> req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        String profileId = req != null ? (req.get("profileId") != null ? req.get("profileId") : req.get("targetProfileId")) : null;
        return ResponseEntity.ok(ApiResponse.success(connectionService.cancelConnectionRequest(profileId, resolveUserIdentifier(userEmail, userId))));
    }

    @PostMapping("/toggle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleConnection(
            @RequestBody(required = false) Map<String, String> req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        String profileId = req != null ? req.get("profileId") : null;
        return ResponseEntity.ok(ApiResponse.success(connectionService.toggleConnection(profileId, resolveUserIdentifier(userEmail, userId))));
    }

    @PostMapping("/follow")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleFollow(
            @RequestBody(required = false) Map<String, String> req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        String profileId = req != null ? req.get("profileId") : null;
        return ResponseEntity.ok(ApiResponse.success(connectionService.toggleFollow(profileId, resolveUserIdentifier(userEmail, userId))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> connectTalent(
            @RequestBody ConnectionRequestDto req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(connectionService.connectTalent(req, resolveUserIdentifier(userEmail, userId))));
    }
}
