package com.social.controller;

import com.social.dto.request.ProfileUpdateRequest;
import com.social.dto.response.ApiResponse;
import com.social.dto.response.ProfileResponse;
import com.social.security.CurrentUser;
import com.social.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
@Tag(name = "Profile", description = "Endpoints for managing and viewing artist profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "Get user profile", description = "Reads a profile by userId subject to visibility. Hides contact info (phone/email) from unauthorized viewers.")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @PathVariable("userId") String userId,
            @CurrentUser(required = false) String viewerUserId) {
        ProfileResponse profile = profileService.getProfile(userId, viewerUserId);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @Operation(summary = "Update signed-in user's profile", description = "Updates or creates the profile of the currently authenticated user.")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateMyProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            @CurrentUser String currentUserId) {
        ProfileResponse updatedProfile = profileService.updateMyProfile(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", updatedProfile));
    }
}
