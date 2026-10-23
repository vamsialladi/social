package com.truehunt.controller;

import com.truehunt.config.UserPrincipal;
import com.truehunt.dto.request.UpdateProfileRequest;
import com.truehunt.dto.response.ApiResponse;
import com.truehunt.dto.response.ProfileResponse;
import com.truehunt.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @PathVariable String userId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        String currentUserId = currentUser != null ? currentUser.getId() : null;
        ProfileResponse profile = profileService.getProfile(userId, currentUserId);
        return ResponseEntity.ok(ApiResponse.of(profile));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody UpdateProfileRequest request) {

        ProfileResponse updated = profileService.updateMyProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }
}
