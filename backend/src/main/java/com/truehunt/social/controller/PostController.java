package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.model.PostEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.PostRepository;
import com.truehunt.social.repository.ProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class PostController {

    private final PostRepository postRepository;
    private final ProfileRepository profileRepository;
    private final NotificationRepository notificationRepository;

    public PostController(PostRepository postRepository, ProfileRepository profileRepository, NotificationRepository notificationRepository) {
        this.postRepository = postRepository;
        this.profileRepository = profileRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/api/posts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPosts(@RequestParam(required = false) String type) {
        List<PostEntity> posts;
        if ("opportunity".equalsIgnoreCase(type)) {
            posts = postRepository.findAllByTypeOrderByCreatedAtDesc("opportunity");
        } else if ("all".equalsIgnoreCase(type)) {
            posts = postRepository.findAllByOrderByCreatedAtDesc();
        } else {
            posts = postRepository.findAllByTypeNotOrderByCreatedAtDesc("opportunity");
        }

        List<Map<String, Object>> dtoList = posts.stream()
                .map(PostEntity::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtoList));
    }

    @GetMapping("/api/opportunities")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOpportunities() {
        List<PostEntity> opps = postRepository.findAllByTypeOrderByCreatedAtDesc("opportunity");
        List<Map<String, Object>> dtoList = opps.stream()
                .map(PostEntity::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtoList));
    }

    @PostMapping("/api/posts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPost(@RequestBody Map<String, Object> req) {
        PostEntity post = new PostEntity();

        String type = (String) req.getOrDefault("type", "post");
        post.setType(type);
        post.setTitle((String) req.get("title"));
        post.setContent((String) req.get("content"));

        if (req.containsKey("media") && req.get("media") instanceof Map) {
            post.setMedia((Map<String, Object>) req.get("media"));
        }

        // Attach author details from current profile
        Optional<ProfileEntity> myProfile = profileRepository.findByUserId("me");
        if (myProfile.isPresent()) {
            ProfileEntity p = myProfile.get();
            post.setAuthorId(p.getUserId());
            post.setAuthorName(p.getName() != null && !p.getName().isBlank() ? p.getName() : "Anonymous Artist");
            post.setAuthorAvatar(p.getProfileImageUrl());
            post.setAuthorHeadline(p.getTalents() != null && !p.getTalents().isEmpty()
                    ? String.join(" · ", p.getTalents())
                    : "TrueHunt Creator");
        } else {
            post.setAuthorId("me");
            post.setAuthorName("You");
            post.setAuthorHeadline("Creator");
        }

        post.setCreatedAt(LocalDateTime.now());
        PostEntity saved = postRepository.save(post);
        return ResponseEntity.ok(ApiResponse.success(saved.toDto()));
    }

    @PostMapping("/api/opportunities")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOpportunity(@RequestBody Map<String, Object> req) {
        req.put("type", "opportunity");
        return createPost(req);
    }

    @PostMapping("/api/posts/{id}/like")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleLike(@PathVariable Long id) {
        Optional<PostEntity> opt = postRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Post not found"));
        }

        PostEntity post = opt.get();
        post.toggleLike();
        PostEntity saved = postRepository.save(post);

        return ResponseEntity.ok(ApiResponse.success(saved.toDto()));
    }

    @PostMapping("/api/posts/{id}/comments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        Optional<PostEntity> opt = postRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Post not found"));
        }

        String commentText = body.get("text");
        if (commentText == null || commentText.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("EMPTY_COMMENT", "Comment text cannot be empty"));
        }

        Optional<ProfileEntity> myProfile = profileRepository.findByUserId("me");
        String author = myProfile.map(ProfileEntity::getName).filter(n -> !n.isBlank()).orElse("You");

        PostEntity post = opt.get();
        post.addComment(author, commentText);
        PostEntity saved = postRepository.save(post);

        return ResponseEntity.ok(ApiResponse.success(saved.toDto()));
    }

    @PostMapping("/api/posts/{id}/apply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleApply(@PathVariable Long id) {
        Optional<PostEntity> opt = postRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Opportunity not found"));
        }

        PostEntity post = opt.get();
        post.toggleApply();
        PostEntity saved = postRepository.save(post);

        if (saved.isAppliedByMe()) {
            notificationRepository.save(new NotificationEntity(
                    "me",
                    "Application Submitted",
                    "Your profile and demo showreels have been submitted for: " + (post.getTitle() != null && !post.getTitle().isBlank() ? post.getTitle() : "Casting Opportunity"),
                    "application"
            ));
        }

        return ResponseEntity.ok(ApiResponse.success(saved.toDto()));
    }

    @DeleteMapping("/api/posts/{id}")
    public ResponseEntity<ApiResponse<String>> deletePost(@PathVariable Long id) {
        if (postRepository.existsById(id)) {
            postRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Post deleted successfully"));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Post not found"));
    }
}
