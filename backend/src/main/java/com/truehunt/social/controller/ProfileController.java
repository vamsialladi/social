package com.truehunt.social.controller;

import com.truehunt.social.dto.*;
import com.truehunt.social.security.AuthenticatedUserResolver;
import com.truehunt.social.service.PostService;
import com.truehunt.social.service.ProfileService;
import com.truehunt.social.service.ProfileViewService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/profile", "/api/profiles"})
public class ProfileController {

    private final ProfileService profileService;
    private final PostService postService;
    private final ProfileViewService profileViewService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ProfileController(
            ProfileService profileService,
            PostService postService,
            ProfileViewService profileViewService,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.profileService = profileService;
        this.postService = postService;
        this.profileViewService = profileViewService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    private String resolveUserIdentifier(HttpServletRequest request, String userEmail, String userId) {
        String resolvedEmail = authenticatedUserResolver.resolveEmail(request);
        if (resolvedEmail != null && !resolvedEmail.isBlank()) {
            return resolvedEmail.trim();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())
                && auth.getName() != null && !auth.getName().isBlank()) {
            return auth.getName().trim();
        }
        if (userId != null && !userId.isBlank()) {
            return userId.trim();
        }
        if (userEmail != null && !userEmail.isBlank()) {
            return userEmail.trim();
        }
        return null;
    }

    private String requireUserIdentifier(HttpServletRequest request, String userEmail, String userId) {
        String identifier = resolveUserIdentifier(request, userEmail, userId);
        if (identifier == null || identifier.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return identifier;
    }

    @GetMapping("/me/viewers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyProfileViewers(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(profileViewService.getMyViewers(resolveUserIdentifier(request, userEmail, userId))));
    }

    @PostMapping("/{profileId}/views")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recordProfileView(
            @PathVariable String profileId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(profileViewService.recordView(profileId, resolveUserIdentifier(request, userEmail, userId))));
    }

    @GetMapping("/directory")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPublicDirectory() {
        List<Map<String, Object>> directory = profileService.getPublicDirectory();
        return ResponseEntity.ok(ApiResponse.success(directory));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileDto>> getMyProfile(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        ProfileDto profile = profileService.getMyProfile(requireUserIdentifier(request, userEmail, userId));
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @RequestMapping(value = "/me", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileDto>> updateMyProfile(
            @RequestBody ProfileUpdateRequest updates,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        ProfileDto profile = profileService.updateMyProfile(
                updates,
                requireUserIdentifier(request, userEmail, userId)
        );
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<ProfileDto>> getProfileById(
            @PathVariable String userId,
            @RequestHeader(value = "X-User-Email", required = false) String viewerEmail,
            @RequestHeader(value = "X-User-Id", required = false) String viewerId,
            HttpServletRequest request
    ) {
        String viewerIdentifier = resolveUserIdentifier(request, viewerEmail, viewerId);
        ProfileDto profile = profileService.getProfileById(userId, viewerIdentifier);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    // Experience endpoints
    @PostMapping("/experience")
    public ResponseEntity<ApiResponse<ExperienceDto>> addExperience(
            @RequestBody ExperienceDto exp,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        ExperienceDto created = profileService.addExperience(exp, resolveUserIdentifier(request, userEmail, userId));
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PutMapping("/experience/{id}")
    public ResponseEntity<ApiResponse<ExperienceDto>> updateExperience(
            @PathVariable String id,
            @RequestBody ExperienceDto exp,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        ExperienceDto updated = profileService.updateExperience(id, exp, resolveUserIdentifier(request, userEmail, userId));
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/experience/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteExperience(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        Map<String, Object> res = profileService.deleteExperience(id, resolveUserIdentifier(request, userEmail, userId));
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    // Education endpoints
    @PostMapping("/education")
    public ResponseEntity<ApiResponse<EducationDto>> addEducation(
            @RequestBody EducationDto edu,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        EducationDto created = profileService.addEducation(edu, resolveUserIdentifier(request, userEmail, userId));
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PutMapping("/education/{id}")
    public ResponseEntity<ApiResponse<EducationDto>> updateEducation(
            @PathVariable String id,
            @RequestBody EducationDto edu,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        EducationDto updated = profileService.updateEducation(id, edu, resolveUserIdentifier(request, userEmail, userId));
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/education/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteEducation(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        Map<String, Object> res = profileService.deleteEducation(id, resolveUserIdentifier(request, userEmail, userId));
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    // Project endpoints
    @PostMapping("/project")
    public ResponseEntity<ApiResponse<ProjectDto>> addProject(
            @RequestBody ProjectDto project,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        ProjectDto created = profileService.addProject(project, resolveUserIdentifier(request, userEmail, userId));
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PutMapping("/project/{id}")
    public ResponseEntity<ApiResponse<ProjectDto>> updateProject(
            @PathVariable String id,
            @RequestBody ProjectDto project,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        ProjectDto updated = profileService.updateProject(id, project, resolveUserIdentifier(request, userEmail, userId));
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/project/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteProject(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        Map<String, Object> res = profileService.deleteProject(id, resolveUserIdentifier(request, userEmail, userId));
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    // Skills endpoints
    @PostMapping("/skills")
    public ResponseEntity<ApiResponse<ProfileDto>> addSkill(
            @RequestBody SkillRequest req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        ProfileDto updated = profileService.addSkill(req.getSkill(), resolveUserIdentifier(request, userEmail, userId));
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/skills/{skill}")
    public ResponseEntity<ApiResponse<ProfileDto>> deleteSkill(
            @PathVariable String skill,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        ProfileDto updated = profileService.deleteSkill(skill, resolveUserIdentifier(request, userEmail, userId));
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    // Profile Posts & Suggestions
    @GetMapping("/{id}/posts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getProfilePosts(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        List<Map<String, Object>> posts = postService.getPostsByAuthorId(id, resolveUserIdentifier(request, userEmail, userId));
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createProfilePost(
            @RequestBody CreatePostRequest postReq,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request
    ) {
        Map<String, Object> post = postService.createPost(postReq, resolveUserIdentifier(request, userEmail, userId));
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSuggestions(
            @RequestParam(required = false, defaultValue = "") String currentId,
            @RequestParam(required = false, defaultValue = "") String userId
    ) {
        String resolvedId = (currentId != null && !currentId.isBlank()) ? currentId : userId;
        List<Map<String, Object>> suggestions = profileService.getSuggestions(resolvedId);
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    @PostMapping("/reference-check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyReference(@RequestBody(required = false) ReferenceCheckRequest req) {
        String refId = req != null ? req.getReferenceId() : "";
        Map<String, Object> res = profileService.verifyReference(refId);
        return ResponseEntity.ok(ApiResponse.success(res));
    }
}
