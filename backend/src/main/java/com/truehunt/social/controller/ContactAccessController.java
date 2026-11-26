package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.security.AuthenticatedUserResolver;
import com.truehunt.social.service.ContactAccessService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contact-access")
public class ContactAccessController {

    private final ContactAccessService contactAccessService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ContactAccessController(
            ContactAccessService contactAccessService,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.contactAccessService = contactAccessService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestAccess(
            @RequestBody(required = false) Map<String, String> request,
            HttpServletRequest httpRequest
    ) {
        String profileId = request == null ? null : request.get("profileId");
        return ResponseEntity.ok(ApiResponse.success(contactAccessService.requestAccess(
                profileId,
                authenticatedUserResolver.requireEmail(httpRequest)
        )));
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getIncomingRequests(
            HttpServletRequest request
    ) {
        String email = authenticatedUserResolver.resolveEmail(request);
        if (email == null || email.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        return ResponseEntity.ok(ApiResponse.success(
                contactAccessService.getIncomingRequests(email)
        ));
    }

    @GetMapping("/requests/outgoing")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOutgoingRequests(
            HttpServletRequest request
    ) {
        String email = authenticatedUserResolver.resolveEmail(request);
        if (email == null || email.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        return ResponseEntity.ok(ApiResponse.success(
                contactAccessService.getOutgoingRequests(email)
        ));
    }

    @GetMapping("/{profileId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus(
            @PathVariable String profileId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(contactAccessService.getStatus(
                profileId,
                authenticatedUserResolver.requireEmail(request)
        )));
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<Map<String, Object>>> acceptRequest(
            @PathVariable Long requestId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(contactAccessService.acceptRequest(
                requestId,
                authenticatedUserResolver.requireEmail(request)
        )));
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectRequest(
            @PathVariable Long requestId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(contactAccessService.rejectRequest(
                requestId,
                authenticatedUserResolver.requireEmail(request)
        )));
    }
}
