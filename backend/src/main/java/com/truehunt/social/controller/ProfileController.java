package com.truehunt.social.controller;

import com.truehunt.social.dto.*;
import com.truehunt.social.service.PostService;
import com.truehunt.social.service.ProfileService;
import com.truehunt.social.service.ProfileViewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/profile", "/api/profiles"})
public class ProfileController {

    private final ProfileService profileService;
    private final PostService postService;
    private final ProfileViewService profileViewService;

    public ProfileController(
            ProfileService profileService,
            PostService postService,
            ProfileViewService profileViewService
    ) {
        this.profileService = profileService;
        this.postService = postService;
        this.profileViewService = profileViewService;
    }

    @GetMapping("/me/viewers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyProfileViewers(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        return ResponseEntity.ok(ApiResponse.success(profileViewService.getMyViewers(userEmail)));
    }

    @PostMapping("/{profileId}/views")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recordProfileView(
            @PathVariable String profileId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        return ResponseEntity.ok(ApiResponse.success(profileViewService.recordView(profileId, userEmail)));
    }

    @GetMapping("/directory")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPublicDirectory() {
        List<Map<String, Object>> directory = profileService.getPublicDirectory();
        return ResponseEntity.ok(ApiResponse.success(directory));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileDto>> getMyProfile(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        ProfileDto profile = profileService.getMyProfile(userEmail);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @RequestMapping(value = "/me", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<ProfileDto>> updateMyProfile(
            @RequestBody ProfileUpdateRequest updates,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        ProfileDto profile = profileService.updateMyProfile(updates, userEmail);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<ProfileDto>> getProfileById(@PathVariable String userId) {
        ProfileDto profile = profileService.getProfileById(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    // Experience endpoints
    @PostMapping("/experience")
    public ResponseEntity<ApiResponse<ExperienceDto>> addExperience(
            @RequestBody ExperienceDto exp,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        ExperienceDto created = profileService.addExperience(exp, userEmail);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PutMapping("/experience/{id}")
    public ResponseEntity<ApiResponse<ExperienceDto>> updateExperience(
            @PathVariable String id,
            @RequestBody ExperienceDto exp,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        ExperienceDto updated = profileService.updateExperience(id, exp, userEmail);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/experience/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteExperience(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        Map<String, Object> res = profileService.deleteExperience(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    // Education endpoints
    @PostMapping("/education")
    public ResponseEntity<ApiResponse<EducationDto>> addEducation(
            @RequestBody EducationDto edu,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        EducationDto created = profileService.addEducation(edu, userEmail);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PutMapping("/education/{id}")
    public ResponseEntity<ApiResponse<EducationDto>> updateEducation(
            @PathVariable String id,
            @RequestBody EducationDto edu,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        EducationDto updated = profileService.updateEducation(id, edu, userEmail);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/education/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteEducation(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        Map<String, Object> res = profileService.deleteEducation(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    // Project endpoints
    @PostMapping("/project")
    public ResponseEntity<ApiResponse<ProjectDto>> addProject(
            @RequestBody ProjectDto project,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        ProjectDto created = profileService.addProject(project, userEmail);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PutMapping("/project/{id}")
    public ResponseEntity<ApiResponse<ProjectDto>> updateProject(
            @PathVariable String id,
            @RequestBody ProjectDto project,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        ProjectDto updated = profileService.updateProject(id, project, userEmail);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/project/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteProject(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        Map<String, Object> res = profileService.deleteProject(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    // Skills endpoints
    @PostMapping("/skills")
    public ResponseEntity<ApiResponse<ProfileDto>> addSkill(
            @RequestBody SkillRequest req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        ProfileDto updated = profileService.addSkill(req.getSkill(), userEmail);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/skills/{skill}")
    public ResponseEntity<ApiResponse<ProfileDto>> deleteSkill(
            @PathVariable String skill,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        ProfileDto updated = profileService.deleteSkill(skill, userEmail);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    // Profile Posts & Suggestions
    @GetMapping("/{id}/posts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getProfilePosts(@PathVariable String id) {
        List<Map<String, Object>> posts = postService.getPostsByAuthorId(id);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createProfilePost(
            @RequestBody CreatePostRequest postReq,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        Map<String, Object> post = postService.createPost(postReq, userEmail);
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSuggestions(
            @RequestParam(required = false, defaultValue = "") String currentId
    ) {
        List<Map<String, Object>> suggestions = profileService.getSuggestions(currentId);
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    @PostMapping("/reference-check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyReference(@RequestBody ReferenceCheckRequest req) {
        Map<String, Object> res = profileService.verifyReference(req.getReferenceId());
        return ResponseEntity.ok(ApiResponse.success(res));
    }
}
