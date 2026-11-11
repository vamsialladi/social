package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.CommentRequest;
import com.truehunt.social.dto.CreatePostRequest;
import com.truehunt.social.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPosts(
            @RequestParam(required = false, defaultValue = "") String type,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPosts(type, userEmail)));
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPostById(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPostById(id, userEmail)));
    }

    @GetMapping("/opportunities")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOpportunities(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.getOpportunities(userEmail)));
    }

    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPost(
            @RequestBody CreatePostRequest postReq,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.createPost(postReq, userEmail)));
    }

    @PostMapping("/opportunities")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOpportunity(
            @RequestBody CreatePostRequest oppReq,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        oppReq.setType("opportunity");
        return ResponseEntity.ok(ApiResponse.success(postService.createPost(oppReq, userEmail)));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deletePost(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        postService.deletePost(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Post deleted successfully", "id", id)));
    }

    @PutMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateOpportunity(
            @PathVariable String id,
            @RequestBody CreatePostRequest opportunityReq,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.updateOpportunity(id, opportunityReq, userEmail)
        ));
    }

    @PatchMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePost(
            @PathVariable String id,
            @RequestBody CreatePostRequest postReq,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.updatePost(id, postReq, userEmail)
        ));
    }

    @PostMapping("/posts/{id}/like")
    public ResponseEntity<ApiResponse<Map<String, Object>>> likePost(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.likePost(id, userEmail)));
    }

    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> commentOnPost(
            @PathVariable String id,
            @Valid @RequestBody CommentRequest req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.commentOnPost(id, req.getText(), userEmail)));
    }

    @PostMapping("/posts/{id}/apply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyToOpportunity(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(postService.applyToOpportunity(id)));
    }
}
