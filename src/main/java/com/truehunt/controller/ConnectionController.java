package com.truehunt.controller;

import com.truehunt.config.UserPrincipal;
import com.truehunt.dto.request.ConnectionActionRequest;
import com.truehunt.dto.request.ConnectionRequestDto;
import com.truehunt.dto.response.ApiResponse;
import com.truehunt.dto.response.ConnectionResponse;
import com.truehunt.dto.response.ProfileResponse;
import com.truehunt.service.ConnectionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

    @Autowired
    private ConnectionService connectionService;

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<ProfileResponse>>> getSuggestions(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<ProfileResponse> suggestions = connectionService.getSuggestions(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.of(suggestions));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConnectionResponse>> requestConnection(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ConnectionRequestDto request) {

        ConnectionResponse connection = connectionService.requestConnection(currentUser.getId(), request.getRecipientId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(connection));
    }

    @PatchMapping("/{connectionId}")
    public ResponseEntity<ApiResponse<ConnectionResponse>> updateConnectionStatus(
            @PathVariable String connectionId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ConnectionActionRequest request) {

        ConnectionResponse response = connectionService.updateConnectionStatus(currentUser.getId(), connectionId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConnectionResponse>>> getAcceptedConnections(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<ConnectionResponse> connections = connectionService.getAcceptedConnections(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.of(connections));
    }
}
