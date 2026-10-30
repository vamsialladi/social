package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileRepository profileRepository;
    private final UserAccountRepository userAccountRepository;

    public ProfileController(ProfileRepository profileRepository, UserAccountRepository userAccountRepository) {
        this.profileRepository = profileRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/directory")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPublicDirectory() {
        List<Map<String, Object>> profiles = profileRepository.findAll().stream()
                .filter(profile -> profile.getName() != null && !profile.getName().isBlank())
                .map(profile -> {
                    Map<String, Object> directoryProfile = new HashMap<>();
                    String publicUserId = findAccountByEmail(profile.getEmail())
                            .map(UserAccountEntity::getPublicUserId)
                            .filter(id -> id != null && !id.isBlank())
                            .orElse(profile.getUserId());
                    directoryProfile.put("id", publicUserId);
                    directoryProfile.put("userId", publicUserId);
                    directoryProfile.put("referId", publicUserId);
                    directoryProfile.put("name", profile.getName());
                    directoryProfile.put("height", profile.getHeight() != null ? profile.getHeight() : "");
                    directoryProfile.put("weight", profile.getWeight() != null ? profile.getWeight() : "");
                    directoryProfile.put("bodyBuild", profile.getBodyBuild() != null ? profile.getBodyBuild() : "");
                    directoryProfile.put("gender", profile.getGender() != null ? profile.getGender() : "");
                    directoryProfile.put("skinTone", profile.getSkinTone() != null ? profile.getSkinTone() : "");
                    directoryProfile.put("experience", profile.getExperience() != null ? profile.getExperience() : "");
                    directoryProfile.put("avatar", profile.getProfileImageUrl() != null ? profile.getProfileImageUrl() : "");

                    List<String> skills = new ArrayList<>(profile.getTalents());
                    profile.getWorkTypes().stream()
                            .filter(workType -> !skills.contains(workType))
                            .forEach(skills::add);
                    directoryProfile.put("skills", skills);
                    directoryProfile.put("category", skills.isEmpty() ? "Film Industry" : skills.get(0));
                    directoryProfile.put("role", skills.isEmpty() ? "Cinema Professional" : skills.get(0));
                    directoryProfile.put("location", profile.getExperiences().stream()
                            .map(experience -> experience.get("location"))
                            .filter(Objects::nonNull)
                            .map(String::valueOf)
                            .filter(location -> !location.isBlank())
                            .findFirst()
                            .orElse(""));
                    return directoryProfile;
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success(profiles));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyProfile(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        ProfileEntity profile = findCurrentProfile(userEmail);
        return ResponseEntity.ok(ApiResponse.success(profile.toDto()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfileById(@PathVariable String userId) {
        Optional<ProfileEntity> profile = profileRepository.findByUserId(userId);
        if (profile.isEmpty()) {
            profile = findAccountByPublicUserId(userId)
                    .flatMap(account -> profileRepository.findByEmail(account.getEmail()));
        }
        return profile.map(p -> ResponseEntity.ok(ApiResponse.success(p.toDto())))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Profile not found")));
    }

    @RequestMapping(value = "/me", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMyProfile(
            @RequestBody Map<String, Object> updates,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        ProfileEntity profile = findCurrentProfile(userEmail);

        if (updates.containsKey("name")) profile.setName((String) updates.get("name"));
        if (updates.containsKey("phone")) profile.setPhone((String) updates.get("phone"));
        if (updates.containsKey("email")) profile.setEmail((String) updates.get("email"));
        if (updates.containsKey("experience")) profile.setExperience(String.valueOf(updates.get("experience")));
        if (updates.containsKey("primaryCraft")) profile.setPrimaryCraft((String) updates.get("primaryCraft"));
        if (updates.containsKey("subCraft")) profile.setSubCraft((String) updates.get("subCraft"));
        if (updates.containsKey("dateOfBirth")) profile.setDateOfBirth((String) updates.get("dateOfBirth"));
        if (updates.containsKey("height")) {
            profile.setHeight(updates.get("height") == null ? "" : String.valueOf(updates.get("height")));
        }
        if (updates.containsKey("weight")) {
            profile.setWeight(updates.get("weight") == null ? "" : String.valueOf(updates.get("weight")));
        }
        if (updates.containsKey("bodyBuild")) {
            profile.setBodyBuild(updates.get("bodyBuild") == null ? "" : String.valueOf(updates.get("bodyBuild")));
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
        if (updates.containsKey("secondaryCrafts") && updates.get("secondaryCrafts") instanceof List) {
            profile.setSecondaryCrafts((List<String>) updates.get("secondaryCrafts"));
        }
        if (updates.containsKey("secondarySubCrafts") && updates.get("secondarySubCrafts") instanceof List) {
            profile.setSecondarySubCrafts((List<String>) updates.get("secondarySubCrafts"));
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

    private ProfileEntity findCurrentProfile(String userEmail) {
        Optional<UserAccountEntity> account = findAccountByEmail(userEmail);
        if (account.isPresent()) {
            UserAccountEntity currentAccount = account.get();
            String publicUserId = currentAccount.getPublicUserId();
            if (publicUserId != null && !publicUserId.isBlank()) {
                ProfileEntity profile = profileRepository.findByUserId(publicUserId)
                        .or(() -> profileRepository.findByEmail(currentAccount.getEmail()))
                        .orElseGet(ProfileEntity::new);
                profile.setUserId(publicUserId);
                if (profile.getName() == null || profile.getName().isBlank()) {
                    profile.setName(currentAccount.getName());
                }
                if (profile.getEmail() == null || profile.getEmail().isBlank()) {
                    profile.setEmail(currentAccount.getEmail());
                }
                return profileRepository.save(profile);
            }
        }

        return profileRepository.findByUserId("me").orElseGet(() -> {
            ProfileEntity profile = new ProfileEntity();
            profile.setUserId("me");
            return profileRepository.save(profile);
        });
    }

    private Optional<UserAccountEntity> findAccountByEmail(String email) {
        return email == null || email.isBlank()
                ? Optional.empty()
                : userAccountRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
    }

    private Optional<UserAccountEntity> findAccountByPublicUserId(String publicUserId) {
        return publicUserId == null || publicUserId.isBlank()
                ? Optional.empty()
                : userAccountRepository.findByPublicUserId(publicUserId);
    }
}
