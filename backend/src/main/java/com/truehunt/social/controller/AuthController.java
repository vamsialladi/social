package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.repository.ProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ProfileRepository profileRepository;

    public AuthController(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        Optional<ProfileEntity> profile = profileRepository.findByUserId("me");

        Map<String, Object> authData = new HashMap<>();
        authData.put("token", "truehunt-session-" + UUID.randomUUID());
        authData.put("userId", "me");
        authData.put("email", email != null ? email : (profile.map(ProfileEntity::getEmail).orElse("user@truehunt.com")));
        profile.ifPresent(p -> authData.put("profile", p.toDto()));

        return ResponseEntity.ok(ApiResponse.success(authData));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody Map<String, String> data) {
        String email = data.get("email");
        String name = data.get("name");

        ProfileEntity profile = profileRepository.findByUserId("me").orElseGet(() -> {
            ProfileEntity p = new ProfileEntity();
            p.setUserId("me");
            return p;
        });

        if (email != null) profile.setEmail(email);
        if (name != null) profile.setName(name);
        profileRepository.save(profile);

        Map<String, Object> authData = new HashMap<>();
        authData.put("token", "truehunt-session-" + UUID.randomUUID());
        authData.put("userId", "me");
        authData.put("profile", profile.toDto());

        return ResponseEntity.ok(ApiResponse.success(authData));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me() {
        Optional<ProfileEntity> profile = profileRepository.findByUserId("me");
        Map<String, Object> user = new HashMap<>();
        user.put("userId", "me");
        user.put("email", profile.map(ProfileEntity::getEmail).orElse("chandrabose@truehunt.com"));
        profile.ifPresent(p -> user.put("profile", p.toDto()));
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
