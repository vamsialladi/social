package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.CommentRequest;
import com.truehunt.social.dto.CreatePostRequest;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.service.InvestorAccessService;
import com.truehunt.social.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private void requireAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("Sign in to view or create casting profiles.");
        }
    }

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPosts(
            @RequestParam(required = false, defaultValue = "") String type,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        if ("casting".equalsIgnoreCase(type)) {
            requireAuthenticatedUser();
        }
        return ResponseEntity.ok(ApiResponse.success(postService.getPosts(type, resolveIdentity(userEmail, userId))));
    }

    @GetMapping("/posts/{id}/likes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPostLikes(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPostLikes(id, resolveIdentity(userEmail, userId))));
    }

    @GetMapping("/posts/saved")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSavedPosts(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.getSavedPosts(resolveIdentity(userEmail, userId))));
    }

    @GetMapping("/posts/hidden")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHiddenContent(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.getHiddenContent(resolveIdentity(userEmail, userId))));
    }

    @GetMapping("/reports/me")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyReports(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.getMyReports(resolveIdentity(userEmail, userId))));
    }

    @DeleteMapping("/reports/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteMyReport(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.deleteMyReport(id, resolveIdentity(userEmail, userId))));
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPostById(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        Map<String, Object> post = postService.getPostById(id, resolveIdentity(userEmail, userId));
        if ("casting".equalsIgnoreCase(String.valueOf(post.get("type")))) {
            requireAuthenticatedUser();
        }
        return ResponseEntity.ok(ApiResponse.success(post));
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
        if (postReq != null && "casting".equalsIgnoreCase(postReq.getType())) {
            requireAuthenticatedUser();
            postReq.setAudience("public");
        }
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

    @PostMapping("/posts/{id}/share")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sharePost(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.sharePost(id, resolveIdentity(userEmail, userId))));
    }

    @PostMapping("/posts/{id}/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> savePost(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.savePost(id, resolveIdentity(userEmail, userId))));
    }

    @DeleteMapping("/posts/{id}/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unsavePost(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.unsavePost(id, resolveIdentity(userEmail, userId))));
    }

    @PostMapping("/posts/{id}/report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reportPost(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.reportPost(id, req, resolveIdentity(userEmail, userId))));
    }

    @PostMapping("/posts/{id}/not-interested")
    public ResponseEntity<ApiResponse<Map<String, Object>>> hidePost(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.hidePost(id, resolveIdentity(userEmail, userId))));
    }

    @DeleteMapping("/posts/{id}/not-interested")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unhidePost(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.unhidePost(id, resolveIdentity(userEmail, userId))));
    }

    @PostMapping("/posts/hide-author/{authorId:.+}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> hideAuthor(
            @PathVariable String authorId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.hideAuthor(authorId, resolveIdentity(userEmail, userId))));
    }

    @DeleteMapping("/posts/hide-author/{authorId:.+}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unhideAuthor(
            @PathVariable String authorId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.unhideAuthor(authorId, resolveIdentity(userEmail, userId))));
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

    @DeleteMapping("/posts/{id}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteComment(
            @PathVariable String id,
            @PathVariable String commentId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.deleteComment(id, commentId, resolveIdentity(userEmail, userId))
        ));
    }

    @PostMapping("/posts/{id}/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleCommentLike(
            @PathVariable String id,
            @PathVariable String commentId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.toggleCommentLike(id, commentId, resolveIdentity(userEmail, userId))
        ));
    }

    @PostMapping("/posts/{id}/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<Map<String, Object>>> replyToComment(
            @PathVariable String id,
            @PathVariable String commentId,
            @Valid @RequestBody CommentRequest req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.replyToComment(id, commentId, req.getText(), resolveIdentity(userEmail, userId))
        ));
    }

    @DeleteMapping("/posts/{id}/comments/{commentId}/replies/{replyId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteCommentReply(
            @PathVariable String id,
            @PathVariable String commentId,
            @PathVariable String replyId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.deleteCommentReply(
                        id,
                        commentId,
                        replyId,
                        resolveIdentity(userEmail, userId)
                )
        ));
    }

    @PostMapping("/posts/{id}/apply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyToOpportunity(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.applyToOpportunity(id, resolveIdentity(userEmail, userId), body)
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
