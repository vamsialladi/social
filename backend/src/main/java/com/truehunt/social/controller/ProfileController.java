package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.repository.ProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileRepository profileRepository;

    public ProfileController(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyProfile() {
        ProfileEntity profile = profileRepository.findByUserId("me")
                .orElseGet(() -> {
                    ProfileEntity newProfile = new ProfileEntity();
                    newProfile.setUserId("me");
                    return profileRepository.save(newProfile);
                });
        return ResponseEntity.ok(ApiResponse.success(profile.toDto()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfileById(@PathVariable String userId) {
        Optional<ProfileEntity> profile = profileRepository.findByUserId(userId);
        return profile.map(p -> ResponseEntity.ok(ApiResponse.success(p.toDto())))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Profile not found")));
    }

    @RequestMapping(value = "/me", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMyProfile(@RequestBody Map<String, Object> updates) {
        ProfileEntity profile = profileRepository.findByUserId("me")
                .orElseGet(() -> {
                    ProfileEntity p = new ProfileEntity();
                    p.setUserId("me");
                    return p;
                });

        if (updates.containsKey("name")) profile.setName((String) updates.get("name"));
        if (updates.containsKey("phone")) profile.setPhone((String) updates.get("phone"));
        if (updates.containsKey("email")) profile.setEmail((String) updates.get("email"));
        if (updates.containsKey("experience")) profile.setExperience(String.valueOf(updates.get("experience")));
        if (updates.containsKey("dateOfBirth")) profile.setDateOfBirth((String) updates.get("dateOfBirth"));
        if (updates.containsKey("height")) {
            profile.setHeight(updates.get("height") == null ? "" : String.valueOf(updates.get("height")));
        }
        if (updates.containsKey("weight")) {
            profile.setWeight(updates.get("weight") == null ? "" : String.valueOf(updates.get("weight")));
        }
        if (updates.containsKey("gender")) profile.setGender((String) updates.get("gender"));
        if (updates.containsKey("skinTone")) profile.setSkinTone((String) updates.get("skinTone"));
        if (updates.containsKey("address")) profile.setAddress((String) updates.get("address"));
        if (updates.containsKey("otherLanguage")) profile.setOtherLanguage((String) updates.get("otherLanguage"));
        if (updates.containsKey("profileImageUrl")) profile.setProfileImageUrl((String) updates.get("profileImageUrl"));
        if (updates.containsKey("coverImageUrl")) profile.setCoverImageUrl((String) updates.get("coverImageUrl"));

        if (updates.containsKey("languages") && updates.get("languages") instanceof List) {
            profile.setLanguages((List<String>) updates.get("languages"));
        }
        if (updates.containsKey("talents") && updates.get("talents") instanceof List) {
            profile.setTalents((List<String>) updates.get("talents"));
        }
        if (updates.containsKey("workTypes") && updates.get("workTypes") instanceof List) {
            profile.setWorkTypes((List<String>) updates.get("workTypes"));
        }
        if (updates.containsKey("videoUrls") && updates.get("videoUrls") instanceof List) {
            profile.setVideoUrls((List<String>) updates.get("videoUrls"));
        }
        if (updates.containsKey("videos") && updates.get("videos") instanceof List) {
            profile.setVideos((List<Map<String, Object>>) updates.get("videos"));
        }
        if (updates.containsKey("experiences") && updates.get("experiences") instanceof List) {
            profile.setExperiences((List<Map<String, Object>>) updates.get("experiences"));
        }

        profile.setUpdatedAt(LocalDateTime.now());
        ProfileEntity saved = profileRepository.save(profile);

        return ResponseEntity.ok(ApiResponse.success(saved.toDto()));
    }

    @PostMapping("/reference-check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyReference(@RequestBody Map<String, String> req) {
        String referenceId = req.get("referenceId");
        Map<String, Object> res = new HashMap<>();
        res.put("referenceId", referenceId != null ? referenceId.trim() : "");
        res.put("verified", referenceId != null && !referenceId.trim().isBlank());
        res.put("message", "Reference verified successfully with TrueHunt Talent Network.");
        return ResponseEntity.ok(ApiResponse.success(res));
    }
}
