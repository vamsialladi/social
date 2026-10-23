package com.truehunt.controller;

import com.truehunt.config.UserPrincipal;
import com.truehunt.dto.request.CreatePostRequest;
import com.truehunt.dto.response.ApiResponse;
import com.truehunt.dto.response.PostResponse;
import com.truehunt.model.MediaAttachment;
import com.truehunt.service.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponse>>> getPostsFeed(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {

        List<PostResponse> posts = postService.getPostsFeed(cursor, limit);
        String nextCursor = null;
        if (!posts.isEmpty() && posts.size() >= limit) {
            nextCursor = posts.get(posts.size() - 1).getCreatedAt().toString();
        }

        return ResponseEntity.ok(ApiResponse.of(posts, nextCursor));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreatePostRequest request) {

        PostResponse created = postService.createPost(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @PostMapping("/{postId}/media")
    public ResponseEntity<ApiResponse<PostResponse>> attachMedia(
            @PathVariable String postId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody MediaAttachment media) {

        PostResponse updated = postService.attachMedia(postId, currentUser.getId(), media);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }
}
