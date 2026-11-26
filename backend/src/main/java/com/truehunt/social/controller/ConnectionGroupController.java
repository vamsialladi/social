package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.CreateConnectionGroupRequest;
import com.truehunt.social.dto.CreateConnectionGroupPostRequest;
import com.truehunt.social.service.ConnectionGroupService;
import com.truehunt.social.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/connections/groups")
public class ConnectionGroupController {

    private final ConnectionGroupService connectionGroupService;
    private final ConversationService conversationService;

    public ConnectionGroupController(
            ConnectionGroupService connectionGroupService,
            ConversationService conversationService) {
        this.connectionGroupService = connectionGroupService;
        this.conversationService = conversationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getGroups(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                connectionGroupService.getGroups(resolveOwnerIdentifier(userEmail, userId, authentication))));
    }

    @GetMapping("/banners")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBannerGroups(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                connectionGroupService.getBannerGroups(resolveOwnerIdentifier(userEmail, userId, authentication))));
    }

    @PostMapping("/banners")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBannerGroup(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.createBannerGroup(
                resolveOwnerIdentifier(userEmail, userId, authentication))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createGroup(
            @RequestBody CreateConnectionGroupRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.createGroup(
                resolveOwnerIdentifier(userEmail, userId, authentication),
                request == null ? null : request.getName(),
                request == null ? null : request.getMemberProfileIds(),
                request == null ? null : request.getImageUrl())));
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateGroup(
            @PathVariable Long groupId,
            @RequestBody CreateConnectionGroupRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.updateGroup(
                resolveOwnerIdentifier(userEmail, userId, authentication),
                groupId,
                request == null ? null : request.getName(),
                request == null ? null : request.getMemberProfileIds(),
                request == null ? null : request.getImageUrl())));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addMembers(
            @PathVariable Long groupId,
            @RequestBody(required = false) CreateConnectionGroupRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.addMembers(
                resolveOwnerIdentifier(userEmail, userId, authentication),
                groupId,
                request == null ? null : request.getMemberProfileIds())));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteGroup(
            @PathVariable Long groupId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        connectionGroupService.deleteGroup(
                resolveOwnerIdentifier(userEmail, userId, authentication),
                groupId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "id", String.valueOf(groupId),
                "message", "Group deleted successfully")));
    }

    @PostMapping("/{groupId}/follow")
    public ResponseEntity<ApiResponse<Map<String, Object>>> followGroup(
            @PathVariable Long groupId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.followGroup(
                resolveOwnerIdentifier(userEmail, userId, authentication), groupId)));
    }

    @DeleteMapping("/{groupId}/follow")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unfollowGroup(
            @PathVariable Long groupId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.unfollowGroup(
                resolveOwnerIdentifier(userEmail, userId, authentication), groupId)));
    }

    @GetMapping("/{groupId}/posts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGroupPosts(
            @PathVariable Long groupId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.getGroupPosts(
                resolveOwnerIdentifier(userEmail, userId, authentication), groupId)));
    }

    @PostMapping("/{groupId}/posts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createGroupPost(
            @PathVariable Long groupId,
            @RequestBody(required = false) CreateConnectionGroupPostRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.createGroupPost(
                resolveOwnerIdentifier(userEmail, userId, authentication),
                groupId,
                request == null ? null : request.getContent(),
                request == null ? null : request.getMediaUrl(),
                request == null ? null : request.getMediaType())));
    }

    @DeleteMapping("/{groupId}/posts/{postId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteGroupPost(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        connectionGroupService.deleteGroupPost(
                resolveOwnerIdentifier(userEmail, userId, authentication), groupId, postId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "id", String.valueOf(postId),
                "message", "Group update deleted successfully")));
    }

    @PostMapping("/{groupId}/conversation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> openGroupConversation(
                    @PathVariable Long groupId,
                    @RequestHeader(value = "X-User-Email", required = false) String userEmail,
                    @RequestHeader(value = "X-User-Id", required = false) String userId,
                    Authentication authentication) {
                String ownerIdentifier = resolveOwnerIdentifier(userEmail, userId, authentication);
                return ResponseEntity.ok(ApiResponse.success(conversationService.getOrCreateGroupConversation(
                        connectionGroupService.getGroupForMessaging(ownerIdentifier, groupId),
                        ownerIdentifier)));
    }

    private String resolveOwnerIdentifier(String userEmail, String userId, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        if (userEmail != null && !userEmail.isBlank()) return userEmail.trim();
        if (userId != null && !userId.isBlank()) return userId.trim();
        return null;
    }
}
