package com.social.controller;

import com.social.dto.request.PostCreateRequest;
import com.social.dto.request.PostReportRequest;
import com.social.dto.request.PostUpdateRequest;
import com.social.dto.response.ApiResponse;
import com.social.dto.response.CursorPageResponse;
import com.social.dto.response.PostResponse;
import com.social.security.CurrentUser;
import com.social.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Posts and Articles", description = "Endpoints for creating, managing, and browsing the feed of posts and articles")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @Operation(summary = "Get home feed posts & articles", description = "Retrieves visible posts and articles (excluding opportunities) ordered newest first with cursor pagination.")
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<PostResponse>>> getFeed(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @CurrentUser(required = false) String viewerUserId) {
        CursorPageResponse<PostResponse> feed = postService.getFeed(cursor, limit, viewerUserId);
        return ResponseEntity.ok(ApiResponse.ok(feed));
    }

    @Operation(summary = "Get post or article by ID", description = "Retrieves a single post by ID subject to visibility.")
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(
            @PathVariable("postId") String postId,
            @CurrentUser(required = false) String viewerUserId) {
        PostResponse post = postService.getPostById(postId, viewerUserId);
        return ResponseEntity.ok(ApiResponse.ok(post));
    }

    @Operation(summary = "Create post or article", description = "Publishes a new post or article. Articles require a title; both require non-empty content.")
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @Valid @RequestBody PostCreateRequest request,
            @CurrentUser String authorId) {
        PostResponse createdPost = postService.createPost(request, authorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Post published successfully", createdPost));
    }

    @Operation(summary = "Update post or article", description = "Allows the original author to edit their own post or article.")
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable("postId") String postId,
            @Valid @RequestBody PostUpdateRequest request,
            @CurrentUser String currentUserId) {
        PostResponse updatedPost = postService.updatePost(postId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Post updated successfully", updatedPost));
    }

    @Operation(summary = "Delete post or article", description = "Allows the original author to delete their own post.")
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable("postId") String postId,
            @CurrentUser String currentUserId) {
        postService.deletePost(postId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Post deleted successfully", null));
    }

    @Operation(summary = "Report post for spam or abuse", description = "Submits a report on a post. High report frequency triggers automatic spam suppression.")
    @PostMapping("/{postId}/report")
    public ResponseEntity<ApiResponse<Void>> reportPost(
            @PathVariable("postId") String postId,
            @Valid @RequestBody(required = false) PostReportRequest request,
            @CurrentUser String reporterUserId) {
        postService.reportPost(postId, request, reporterUserId);
        return ResponseEntity.ok(ApiResponse.ok("Post reported. Thank you for helping keep the community safe.", null));
    }
}
