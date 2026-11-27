package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.CreateConnectionGroupPostActivityRequest;
import com.truehunt.social.dto.CreateConnectionGroupRequest;
import com.truehunt.social.dto.CreateConnectionGroupPostRequest;
import com.truehunt.social.dto.OrganizationPageAdminRequest;
import com.truehunt.social.dto.OrganizationPageBlockedMemberRequest;
import com.truehunt.social.dto.RespondOrganizationPageSubscriptionRequest;
import com.truehunt.social.service.ConnectionGroupService;
import com.truehunt.social.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
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

    @GetMapping("/organization-pages")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrganizationPages(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(connectionGroupService.getOrganizationPages(
                        resolveOwnerIdentifier(userEmail, userId, authentication))));
    }

    @GetMapping("/organization-pages/subscription-requests")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrganizationPageSubscriptionRequests(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.getOrganizationPageSubscriptionRequests(
                resolveOwnerIdentifier(userEmail, userId, authentication))));
    }

    @PostMapping("/organization-pages/subscription-requests/{requestId}/respond")
    public ResponseEntity<ApiResponse<Map<String, Object>>> respondToOrganizationPageSubscriptionRequest(
            @PathVariable Long requestId,
            @RequestBody(required = false) RespondOrganizationPageSubscriptionRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                connectionGroupService.respondToOrganizationPageSubscriptionRequest(
                        resolveOwnerIdentifier(userEmail, userId, authentication),
                        requestId,
                        request == null ? null : request.getDecision())));
    }

    @PostMapping("/organization-pages")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOrganizationPage(
            @RequestBody(required = false) CreateConnectionGroupRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.createOrganizationPage(
                resolveOwnerIdentifier(userEmail, userId, authentication),
                request == null ? null : request.getName(),
                request == null ? null : request.getPublicUrl(),
                request == null ? null : request.getWebsite(),
                request == null ? null : request.getIndustry(),
                request == null ? null : request.getTagline(),
                request == null ? null : request.getImageUrl(),
                request == null ? null : request.getBannerUrl())));
    }

    @PutMapping("/organization-pages/{pageId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateOrganizationPage(
            @PathVariable Long pageId,
            @RequestBody(required = false) CreateConnectionGroupRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.updateOrganizationPage(
                resolveOwnerIdentifier(userEmail, userId, authentication),
                pageId,
                request == null ? null : request.getName(),
                request == null ? null : request.getPublicUrl(),
                request == null ? null : request.getWebsite(),
                request == null ? null : request.getIndustry(),
                request == null ? null : request.getTagline(),
                request == null ? null : request.getImageUrl(),
                request == null ? null : request.getBannerUrl())));
    }

    @DeleteMapping("/organization-pages/{pageId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteOrganizationPage(
            @PathVariable Long pageId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        connectionGroupService.deleteOrganizationPage(
                resolveOwnerIdentifier(userEmail, userId, authentication), pageId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "id", String.valueOf(pageId),
                "message", "Organization page deleted successfully")));
    }

    @GetMapping("/organization-pages/{pageId}/admins")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrganizationPageAdmins(
            @PathVariable Long pageId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.getOrganizationPageAdmins(
                resolveOwnerIdentifier(userEmail, userId, authentication), pageId)));
    }

    @PostMapping("/organization-pages/{pageId}/admins")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addOrganizationPageAdmin(
            @PathVariable Long pageId,
            @RequestBody(required = false) OrganizationPageAdminRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.addOrganizationPageAdmin(
                resolveOwnerIdentifier(userEmail, userId, authentication),
                pageId,
                request == null ? null : request.getProfileId())));
    }

    @DeleteMapping("/organization-pages/{pageId}/admins/{adminId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeOrganizationPageAdmin(
            @PathVariable Long pageId,
            @PathVariable String adminId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.removeOrganizationPageAdmin(
                resolveOwnerIdentifier(userEmail, userId, authentication), pageId, adminId)));
    }

    @GetMapping("/organization-pages/{pageId}/blocked-members")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrganizationPageBlockedMembers(
            @PathVariable Long pageId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.getOrganizationPageBlockedMembers(
                resolveOwnerIdentifier(userEmail, userId, authentication), pageId)));
    }

    @PostMapping("/organization-pages/{pageId}/blocked-members")
    public ResponseEntity<ApiResponse<Map<String, Object>>> blockOrganizationPageMember(
            @PathVariable Long pageId,
            @RequestBody(required = false) OrganizationPageBlockedMemberRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.blockOrganizationPageMember(
                resolveOwnerIdentifier(userEmail, userId, authentication),
                pageId,
                request == null ? null : request.getProfileId())));
    }

    @DeleteMapping("/organization-pages/{pageId}/blocked-members/{profileId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unblockOrganizationPageMember(
            @PathVariable Long pageId,
            @PathVariable String profileId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.unblockOrganizationPageMember(
                resolveOwnerIdentifier(userEmail, userId, authentication), pageId, profileId)));
    }

    @DeleteMapping("/organization-pages/{pageId}/followers/{profileId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeOrganizationPageFollower(
            @PathVariable Long pageId,
            @PathVariable String profileId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.removeOrganizationPageFollower(
                resolveOwnerIdentifier(userEmail, userId, authentication), pageId, profileId)));
    }

    @GetMapping("/organization-pages/{pageId}/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrganizationPageAnalytics(
            @PathVariable Long pageId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.getOrganizationPageAnalytics(
                resolveOwnerIdentifier(userEmail, userId, authentication), pageId)));
    }

    @PostMapping("/organization-pages/{pageId}/visits")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recordOrganizationPageVisit(
            @PathVariable Long pageId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.recordOrganizationPageVisit(
                resolveOwnerIdentifier(userEmail, userId, authentication), pageId)));
    }

    @PostMapping("/organization-pages/{pageId}/subscribe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> subscribeToOrganizationPage(
            @PathVariable Long pageId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.requestOrganizationPageSubscription(
                resolveOwnerIdentifier(userEmail, userId, authentication), pageId)));
    }

    @DeleteMapping("/organization-pages/{pageId}/subscribe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unsubscribeFromOrganizationPage(
            @PathVariable Long pageId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.unsubscribeFromOrganizationPage(
                resolveOwnerIdentifier(userEmail, userId, authentication), pageId)));
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
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(connectionGroupService.getGroupPosts(
                        resolveOwnerIdentifier(userEmail, userId, authentication), groupId)));
    }

    @PostMapping("/{groupId}/posts/{postId}/activities")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recordGroupPostActivity(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @RequestBody(required = false) CreateConnectionGroupPostActivityRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.recordGroupPostActivity(
                resolveOwnerIdentifier(userEmail, userId, authentication),
                groupId,
                postId,
                request == null ? null : request.getType(),
                request == null ? null : request.getContent(),
                request == null ? null : request.getParentActivityId())));
    }

    @DeleteMapping("/{groupId}/posts/{postId}/activities/{activityId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteGroupPostActivity(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @PathVariable Long activityId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.deleteGroupPostActivity(
                resolveOwnerIdentifier(userEmail, userId, authentication),
                groupId,
                postId,
                activityId)));
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

    @PutMapping("/{groupId}/posts/{postId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateGroupPost(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @RequestBody(required = false) CreateConnectionGroupPostRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(connectionGroupService.updateGroupPost(
                resolveOwnerIdentifier(userEmail, userId, authentication),
                groupId,
                postId,
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
