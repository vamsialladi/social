package com.truehunt.social.service;

import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.model.ConnectionGroupEntity;
import com.truehunt.social.model.ConnectionGroupPostEntity;
import com.truehunt.social.model.ConversationEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ConnectionGroupRepository;
import com.truehunt.social.repository.ConnectionGroupPostRepository;
import com.truehunt.social.repository.ConversationRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectionGroupServiceTest {

    @Test
    void createGroupStoresCanonicalIdsForSelectedConnections() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(connections.getConnectedProfiles("owner@example.com")).thenReturn(List.of(
                Map.of("userId", "connection-id", "id", "connection-id", "email", "connection@example.com")));
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConnectionGroupService service = service(groups, accounts, connections);
        Map<String, Object> created = service.createGroup(
                "owner@example.com",
                "  Film crew  ",
                List.of("connection@example.com", "connection-id"),
                "/uploads/group-photo.jpg");

        assertEquals("Film crew", created.get("name"));
        assertEquals("/uploads/group-photo.jpg", created.get("imageUrl"));
        assertEquals(List.of("connection-id"), created.get("memberProfileIds"));
    }

    @Test
    void resolvesLegacyProfileWhenNoUserAccountExists() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ProfileRepository profiles = mock(ProfileRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        ProfileEntity owner = new ProfileEntity();
        owner.setUserId("legacy-owner-id");
        owner.setEmail("owner@example.com");
        owner.setName("Legacy Owner");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setId(7L);
        group.setOwnerId("legacy-owner-id");
        group.setName("Film crew");
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.empty());
        when(accounts.findByPublicUserId("owner@example.com")).thenReturn(Optional.empty());
        when(profiles.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(group));

        ConnectionGroupService service = new ConnectionGroupService(
                groups,
                mock(ConnectionGroupPostRepository.class),
                accounts,
                connections,
                null,
                profiles);

        List<Map<String, Object>> returnedGroups = service.getGroups("owner@example.com");

        assertEquals(1, returnedGroups.size());
        assertEquals(true, returnedGroups.get(0).get("isOwner"));
    }

    @Test
    void createGroupDoesNotRequirePreselectedConnections() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(connections.getConnectedProfiles("owner@example.com")).thenReturn(List.of());
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> created = service(groups, accounts, connections)
                .createGroup("owner@example.com", "Open group", List.of(), null);

        assertEquals(List.of(), created.get("memberProfileIds"));
    }

    @Test
    void createGroupRejectsPeopleOutsideTheOwnersConnections() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(connections.getConnectedProfiles("owner@example.com")).thenReturn(List.of());

        ConnectionGroupService service = service(groups, accounts, connections);

        assertThrows(
                BadRequestException.class,
                () -> service.createGroup("owner@example.com", "Film crew", List.of("other-person-id"), null));
        verify(groups, never()).save(any(ConnectionGroupEntity.class));
    }

    @Test
    void updateGroupPreservesMembersWhenSavingGroupDetails() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository groupPosts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        ConversationRepository conversations = mock(ConversationRepository.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setId(7L);
        group.setOwnerId("owner-id");
        group.setName("Old group");
        group.setMemberProfileIds(List.of("old-member"));
        ConversationEntity conversation = new ConversationEntity();
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findByIdAndOwnerId(7L, "owner-id")).thenReturn(Optional.of(group));
        when(connections.getConnectedProfiles("owner@example.com")).thenReturn(List.of());
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversations.findFirstByConnectionGroupId(7L)).thenReturn(Optional.of(conversation));
        when(conversations.save(any(ConversationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConnectionGroupService service = new ConnectionGroupService(
                groups,
                groupPosts,
                accounts,
                connections,
                conversations);
        Map<String, Object> updated = service.updateGroup(
                "owner@example.com",
                7L,
                "Updated group",
                List.of(),
                "/uploads/updated-group-photo.jpg");

        assertEquals("Updated group", updated.get("name"));
        assertEquals("/uploads/updated-group-photo.jpg", updated.get("imageUrl"));
        assertEquals(List.of("old-member"), updated.get("memberProfileIds"));
        assertEquals("/uploads/updated-group-photo.jpg", conversation.getGroupImageUrl());
        verify(groups).save(group);
        verify(conversations).save(conversation);
    }

    @Test
    void updateGroupPreservesPhotoWhenImageUrlIsOmitted() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setId(7L);
        group.setOwnerId("owner-id");
        group.setName("Film crew");
        group.setImageUrl("/uploads/current-group-photo.jpg");
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findByIdAndOwnerId(7L, "owner-id")).thenReturn(Optional.of(group));
        when(connections.getConnectedProfiles("owner@example.com")).thenReturn(List.of());
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> updated = service(groups, accounts, connections)
                .updateGroup("owner@example.com", 7L, "Updated group", List.of(), null);

        assertEquals("/uploads/current-group-photo.jpg", updated.get("imageUrl"));
    }

    @Test
    void updateGroupClearsPhotoWhenImageUrlIsExplicitlyEmpty() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setId(7L);
        group.setOwnerId("owner-id");
        group.setName("Film crew");
        group.setImageUrl("/uploads/current-group-photo.jpg");
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findByIdAndOwnerId(7L, "owner-id")).thenReturn(Optional.of(group));
        when(connections.getConnectedProfiles("owner@example.com")).thenReturn(List.of());
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> updated = service(groups, accounts, connections)
                .updateGroup("owner@example.com", 7L, "Updated group", List.of(), "");

        assertNull(updated.get("imageUrl"));
    }

    @Test
    void onlyGroupOwnerCanEditGroupMembers() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity otherUser = account(2L, "other@example.com", "other-id");
        when(accounts.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        ConnectionGroupService service = service(groups, accounts, connections);

        assertThrows(
                BadRequestException.class,
                () -> service.updateGroup("other@example.com", 7L, "Changed group", List.of(), null)
        );
        verify(groups, never()).save(any(ConnectionGroupEntity.class));
    }

    @Test
    void groupDirectoryIncludesOtherOwnersGroupsAndViewerOwnershipState() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity viewer = account(2L, "viewer@example.com", "viewer-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setOwnerId("owner-id");
        group.setName("Film crew");
        group.setMemberProfileIds(List.of("member-id"));
        when(accounts.findByEmail("viewer@example.com")).thenReturn(Optional.of(viewer));
        when(groups.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(group));

        List<Map<String, Object>> result =
                service(groups, accounts, connections).getGroups("viewer@example.com");

        assertEquals(1, result.size());
        assertEquals("owner-id", result.get(0).get("ownerId"));
        assertEquals(false, result.get(0).get("isOwner"));
        assertEquals(false, result.get(0).get("isFollowing"));
    }

    @Test
    void followingIsSeparateFromGroupMembersAndCanBeRemoved() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity viewer = account(2L, "viewer@example.com", "viewer-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setOwnerId("owner-id");
        group.setName("Film crew");
        group.setMemberProfileIds(List.of("member-id"));
        when(accounts.findByEmail("viewer@example.com")).thenReturn(Optional.of(viewer));
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConnectionGroupService service = service(groups, accounts, connections);
        Map<String, Object> followed = service.followGroup("viewer@example.com", 7L);
        Map<String, Object> unfollowed = service.unfollowGroup("viewer@example.com", 7L);

        assertEquals(true, followed.get("isFollowing"));
        assertEquals(1, followed.get("followerCount"));
        assertEquals(List.of("member-id", "viewer-id"), followed.get("memberProfileIds"));
        assertEquals(false, unfollowed.get("isFollowing"));
        assertEquals(0, unfollowed.get("followerCount"));
        assertEquals(List.of("member-id"), unfollowed.get("memberProfileIds"));
    }

    @Test
    void groupEditsPreserveMembersWhoFollowedTheGroup() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setId(7L);
        group.setOwnerId("owner-id");
        group.setName("Film crew");
        group.setMemberProfileIds(List.of("old-member", "follower-id"));
        group.setFollowerUserIds(List.of("follower-id"));
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findByIdAndOwnerId(7L, "owner-id")).thenReturn(Optional.of(group));
        when(connections.getConnectedProfiles("owner@example.com")).thenReturn(List.of());
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> updated = service(groups, accounts, connections)
                .updateGroup("owner@example.com", 7L, "Updated group", List.of(), null);

        assertEquals(List.of("old-member", "follower-id"), updated.get("memberProfileIds"));
    }

    @Test
    void groupPostFeedReturnsFullPostsOnlyToOwnersAndFollowers() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity viewer = account(2L, "viewer@example.com", "viewer-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setOwnerId("owner-id");
        group.setFollowerUserIds(List.of("viewer-id"));
        ConnectionGroupPostEntity textPost = new ConnectionGroupPostEntity();
        textPost.setContent("Full text update");
        when(accounts.findByEmail("viewer@example.com")).thenReturn(Optional.of(viewer));
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        when(posts.findAllByGroupIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(textPost));

        Map<String, Object> feed =
                new ConnectionGroupService(groups, posts, accounts, connections)
                        .getGroupPosts("viewer@example.com", 7L);

        assertEquals(true, feed.get("canViewAll"));
        assertEquals("Full text update", ((Map<?, ?>) ((List<?>) feed.get("posts")).get(0)).get("content"));
    }

    @Test
    void nonFollowersReceiveOnlyTheMostRecentPhotoPreview() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity viewer = account(2L, "viewer@example.com", "viewer-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setOwnerId("owner-id");
        ConnectionGroupPostEntity videoPost = new ConnectionGroupPostEntity();
        videoPost.setMediaUrl("/uploads/recent.mp4");
        videoPost.setMediaType("video/mp4");
        videoPost.setContent("Hidden video caption");
        ConnectionGroupPostEntity imagePost = new ConnectionGroupPostEntity();
        imagePost.setMediaUrl("/uploads/recent.jpg");
        imagePost.setMediaType("image/jpeg");
        imagePost.setContent("Hidden image caption");
        when(accounts.findByEmail("viewer@example.com")).thenReturn(Optional.of(viewer));
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        when(posts.findAllByGroupIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(videoPost, imagePost));

        Map<String, Object> feed =
                new ConnectionGroupService(groups, posts, accounts, connections)
                        .getGroupPosts("viewer@example.com", 7L);

        assertEquals(false, feed.get("canViewAll"));
        assertEquals(List.of(), feed.get("posts"));
        Map<?, ?> preview = (Map<?, ?>) feed.get("preview");
        assertEquals("/uploads/recent.jpg", preview.get("imageUrl"));
        assertEquals(false, preview.containsKey("content"));
        assertEquals(false, preview.containsKey("mediaUrl"));
    }

    @Test
    void onlyGroupOwnerCanCreateGroupPosts() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        UserAccountEntity other = account(2L, "other@example.com", "other-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setOwnerId("owner-id");
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(accounts.findByEmail("other@example.com")).thenReturn(Optional.of(other));
        when(groups.findByIdAndOwnerId(7L, "owner-id")).thenReturn(Optional.of(group));
        when(posts.save(any(ConnectionGroupPostEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ConnectionGroupService service = new ConnectionGroupService(groups, posts, accounts, connections);

        Map<String, Object> created =
                service.createGroupPost("owner@example.com", 7L, "New group update", null, null);

        assertEquals("owner-id", created.get("authorId"));
        assertEquals("New group update", created.get("content"));
        assertThrows(
                BadRequestException.class,
                () -> service.createGroupPost("other@example.com", 7L, "Not allowed", null, null));
        verify(posts).save(any(ConnectionGroupPostEntity.class));
    }

    @Test
    void groupPostAuthorCanDeleteTheWholeUpdate() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setId(7L);
        group.setOwnerId("owner-id");
        ConnectionGroupPostEntity post = new ConnectionGroupPostEntity();
        post.setAuthorId("owner-id");
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findByIdAndOwnerId(7L, "owner-id")).thenReturn(Optional.of(group));
        when(posts.findByIdAndGroupId(12L, 7L)).thenReturn(Optional.of(post));

        new ConnectionGroupService(groups, posts, accounts, connections)
                .deleteGroupPost("owner@example.com", 7L, 12L);

        verify(posts).delete(post);
    }

    @Test
    void groupOwnerCannotDeleteAnUpdateWrittenByAnotherAuthor() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setId(7L);
        group.setOwnerId("owner-id");
        ConnectionGroupPostEntity post = new ConnectionGroupPostEntity();
        post.setAuthorId("other-id");
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findByIdAndOwnerId(7L, "owner-id")).thenReturn(Optional.of(group));
        when(posts.findByIdAndGroupId(12L, 7L)).thenReturn(Optional.of(post));

        assertThrows(
                BadRequestException.class,
                () -> new ConnectionGroupService(groups, posts, accounts, connections)
                        .deleteGroupPost("owner@example.com", 7L, 12L));
        verify(posts, never()).delete(any(ConnectionGroupPostEntity.class));
    }

    @Test
    void deletingGroupRemovesTheOwnedGroup() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findByIdAndOwnerId(7L, "owner-id")).thenReturn(Optional.of(group));

        ConnectionGroupService service = service(groups, accounts, connections);
        service.deleteGroup("owner@example.com", 7L);

        verify(groups).delete(group);
    }

    @Test
    void anotherUserCannotDeleteGroup() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity otherUser = account(2L, "other@example.com", "other-id");
        when(accounts.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        ConnectionGroupService service = service(groups, accounts, connections);

        assertThrows(BadRequestException.class, () -> service.deleteGroup("other@example.com", 7L));
        verify(groups, never()).delete(any(ConnectionGroupEntity.class));
    }

    private static UserAccountEntity account(Long id, String email, String publicUserId) {
        UserAccountEntity account = new UserAccountEntity();
        account.setId(id);
        account.setEmail(email);
        account.setPublicUserId(publicUserId);
        return account;
    }

    private static ConnectionGroupService service(
            ConnectionGroupRepository groups,
            UserAccountRepository accounts,
            ConnectionService connections) {
        return new ConnectionGroupService(
                groups,
                mock(ConnectionGroupPostRepository.class),
                accounts,
                connections);
    }
}
