package com.social.controller;

import com.social.config.UserContextHolder;
import com.social.dto.request.SendConnectionRequest;
import com.social.dto.request.UpdateConnectionStatusRequest;
import com.social.dto.response.ApiResponse;
import com.social.dto.response.ConnectionResponse;
import com.social.dto.response.UserProfileResponse;
import com.social.service.ConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
@Tag(name = "Module 6: Network and Connections", description = "Endpoints for discovering artists, sending connection requests, accepting/rejecting, and listing connections")
public class ConnectionController {

    private final ConnectionService connectionService;

    @GetMapping("/suggestions")
    @Operation(summary = "Get connection suggestions", description = "Eligible suggested profiles (excludes self, existing connections/requests, and blocked users)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Suggestions retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required (missing X-User-Id header)")
    })
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getSuggestions(
            @Parameter(description = "Maximum number of suggestions to return (default: 20)")
            @RequestParam(defaultValue = "20") int limit
    ) {
        String currentUserId = UserContextHolder.requireUserId();
        List<UserProfileResponse> suggestions = connectionService.getSuggestions(currentUserId, limit);
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    @PostMapping
    @Operation(summary = "Send a connection request", description = "Send a connection request to a target recipient profile ID. Rejects self-connection and duplicates, creates pending request, creates a notification for recipient.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Connection request sent successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or self-connection attempted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required (missing X-User-Id header)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Recipient user not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Active connection pair already exists")
    })
    public ResponseEntity<ApiResponse<ConnectionResponse>> sendConnectionRequest(
            @Valid @RequestBody SendConnectionRequest request
    ) {
        String currentUserId = UserContextHolder.requireUserId();
        ConnectionResponse response = connectionService.sendConnectionRequest(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Connection request sent successfully", response));
    }

    @PatchMapping("/{connectionId}")
    @Operation(summary = "Accept or reject connection request", description = "Recipient accepts or rejects connection. Server verifies the caller is the recipient of that request.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Connection status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required (missing X-User-Id header)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not the recipient of this request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Connection not found")
    })
    public ResponseEntity<ApiResponse<ConnectionResponse>> updateConnectionStatus(
            @PathVariable String connectionId,
            @Valid @RequestBody UpdateConnectionStatusRequest request
    ) {
        String currentUserId = UserContextHolder.requireUserId();
        ConnectionResponse response = connectionService.updateConnectionStatus(currentUserId, connectionId, request);
        return ResponseEntity.ok(ApiResponse.success("Connection status updated to " + request.getStatus().getValue(), response));
    }

    @GetMapping
    @Operation(summary = "List accepted connections", description = "List accepted connections for the signed-in user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Accepted connections listed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required (missing X-User-Id header)")
    })
    public ResponseEntity<ApiResponse<List<ConnectionResponse>>> getAcceptedConnections() {
        String currentUserId = UserContextHolder.requireUserId();
        List<ConnectionResponse> connections = connectionService.getAcceptedConnections(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(connections));
    }

    @GetMapping("/{connectionId}")
    @Operation(summary = "Get connection by ID", description = "Retrieve connection details if caller is a participant")
    public ResponseEntity<ApiResponse<ConnectionResponse>> getConnectionById(@PathVariable String connectionId) {
        String currentUserId = UserContextHolder.requireUserId();
        ConnectionResponse response = connectionService.getConnectionById(currentUserId, connectionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
