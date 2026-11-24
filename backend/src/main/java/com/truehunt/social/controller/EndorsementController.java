package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.security.AuthenticatedUserResolver;
import com.truehunt.social.service.EndorsementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profiles/{profileId}/endorsements")
public class EndorsementController {

    private final EndorsementService endorsementService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public EndorsementController(
            EndorsementService endorsementService,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.endorsementService = endorsementService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getProfileEndorsements(
            @PathVariable String profileId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                endorsementService.getProfileEndorsements(
                        profileId,
                        authenticatedUserResolver.resolveEmail(request)
                )
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createEndorsement(
            @PathVariable String profileId,
            @RequestBody Map<String, Object> endorsement,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                endorsementService.createEndorsement(
                        profileId,
                        endorsement,
                        authenticatedUserResolver.requireEmail(request)
                )
        ));
    }

    @PutMapping("/{endorsementId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateEndorsement(
            @PathVariable String profileId,
            @PathVariable Long endorsementId,
            @RequestBody Map<String, Object> endorsement,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                endorsementService.updateEndorsement(
                        profileId,
                        endorsementId,
                        endorsement,
                        authenticatedUserResolver.requireEmail(request)
                )
        ));
    }

    @DeleteMapping("/{endorsementId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> deleteEndorsement(
            @PathVariable String profileId,
            @PathVariable Long endorsementId,
            HttpServletRequest request
    ) {
        endorsementService.deleteEndorsement(
                profileId,
                endorsementId,
                authenticatedUserResolver.requireEmail(request)
        );
        return ResponseEntity.ok(ApiResponse.success(Map.of("deleted", true)));
    }
}