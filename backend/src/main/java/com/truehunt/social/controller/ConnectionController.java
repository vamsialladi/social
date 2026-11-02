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
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSuggestions() {
        return ResponseEntity.ok(ApiResponse.success(connectionService.getSuggestions()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConnectedProfiles() {
        return ResponseEntity.ok(ApiResponse.success(connectionService.getConnectedProfiles()));
    }

    @PostMapping("/toggle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleConnection(@RequestBody Map<String, String> req) {
        String profileId = req.get("profileId");
        return ResponseEntity.ok(ApiResponse.success(connectionService.toggleConnection(profileId)));
    }

    @PostMapping("/follow")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleFollow(@RequestBody Map<String, String> req) {
        String profileId = req.get("profileId");
        return ResponseEntity.ok(ApiResponse.success(connectionService.toggleFollow(profileId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> connectTalent(@RequestBody ConnectionRequestDto req) {
        return ResponseEntity.ok(ApiResponse.success(connectionService.connectTalent(req)));
    }
}
