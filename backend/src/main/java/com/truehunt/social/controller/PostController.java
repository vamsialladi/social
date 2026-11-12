package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.CommentRequest;
import com.truehunt.social.dto.CreatePostRequest;
import com.truehunt.social.service.InvestorAccessService;
import com.truehunt.social.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PostController {

    private final PostService postService;
    private final InvestorAccessService investorAccessService;

    public PostController(PostService postService, InvestorAccessService investorAccessService) {
        this.postService = postService;
        this.investorAccessService = investorAccessService;
    }

    private String resolveIdentity(String userEmail, String userId) {
        if (userEmail != null && !userEmail.isBlank()) {
            return userEmail.trim();
        }
        if (userId != null && !userId.isBlank()) {
            return userId.trim();
        }
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return null;
    }

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPosts(
            @RequestParam(required = false, defaultValue = "") String type,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPosts(type, resolveIdentity(userEmail, userId))));
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPostById(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPostById(id, resolveIdentity(userEmail, userId))));
    }

    @GetMapping("/opportunities")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOpportunities(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.getOpportunities(resolveIdentity(userEmail, userId))));
    }

    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPost(
            @RequestBody CreatePostRequest postReq,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.createPost(postReq, resolveIdentity(userEmail, userId))));
    }

    @PostMapping("/opportunities")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOpportunity(
            @RequestBody CreatePostRequest oppReq,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        oppReq.setType("opportunity");
        return ResponseEntity.ok(ApiResponse.success(postService.createPost(oppReq, resolveIdentity(userEmail, userId))));
    }

    @DeleteMapping("/posts/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> deletePost(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        String caller = resolveIdentity(userEmail, userId);
        postService.deletePost(id, caller);
        boolean investorProfileDeleted = investorAccessService
                .deletePublishedListingForDeletedPost(id, caller);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "message", "Post deleted successfully",
                "id", id,
                "investorProfileDeleted", investorProfileDeleted
        )));
    }

    @PutMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateOpportunity(
            @PathVariable String id,
            @RequestBody CreatePostRequest opportunityReq,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.updateOpportunity(id, opportunityReq, resolveIdentity(userEmail, userId))
        ));
    }

    @PatchMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePost(
            @PathVariable String id,
            @RequestBody CreatePostRequest postReq,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.updatePost(id, postReq, resolveIdentity(userEmail, userId))
        ));
    }

    @PostMapping("/posts/{id}/like")
    public ResponseEntity<ApiResponse<Map<String, Object>>> likePost(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.likePost(id, resolveIdentity(userEmail, userId))));
    }

    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> commentOnPost(
            @PathVariable String id,
            @Valid @RequestBody CommentRequest req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.commentOnPost(id, req.getText(), resolveIdentity(userEmail, userId))));
    }

    @PostMapping("/posts/{id}/apply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyToOpportunity(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.applyToOpportunity(id, resolveIdentity(userEmail, userId))
        ));
    }

    @DeleteMapping("/posts/{id}/apply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelOpportunityApplication(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.cancelOpportunityApplication(id, resolveIdentity(userEmail, userId))
        ));
    }
}
