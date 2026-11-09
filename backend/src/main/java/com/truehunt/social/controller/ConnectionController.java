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

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSuggestions(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        return ResponseEntity.ok(ApiResponse.success(connectionService.getSuggestions(userEmail)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConnectedProfiles(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        return ResponseEntity.ok(ApiResponse.success(connectionService.getConnectedProfiles(userEmail)));
    }

    @PostMapping("/toggle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleConnection(
            @RequestBody(required = false) Map<String, String> req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        String profileId = req != null ? req.get("profileId") : null;
        return ResponseEntity.ok(ApiResponse.success(connectionService.toggleConnection(profileId, userEmail)));
    }

    @PostMapping("/follow")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleFollow(
            @RequestBody(required = false) Map<String, String> req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        String profileId = req != null ? req.get("profileId") : null;
        return ResponseEntity.ok(ApiResponse.success(connectionService.toggleFollow(profileId, userEmail)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> connectTalent(
            @RequestBody ConnectionRequestDto req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        return ResponseEntity.ok(ApiResponse.success(connectionService.connectTalent(req, userEmail)));
    }
}
