package com.truehunt.social.service;

import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.model.ConnectionGroupEntity;
import com.truehunt.social.model.ConnectionGroupPostActivityEntity;
import com.truehunt.social.model.ConnectionGroupPostEntity;
import com.truehunt.social.model.OrganizationPageAnalyticsEventEntity;
import com.truehunt.social.model.ConversationEntity;
import com.truehunt.social.model.OrganizationPageSubscriptionRequestEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ConnectionGroupRepository;
import com.truehunt.social.repository.ConnectionGroupPostActivityRepository;
import com.truehunt.social.repository.ConnectionGroupPostRepository;
import com.truehunt.social.repository.OrganizationPageAnalyticsEventRepository;
import com.truehunt.social.repository.OrganizationPageSubscriptionRequestRepository;
import com.truehunt.social.repository.ConversationRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
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
        assertEquals(List.of(), created.get("memberProfileIds"));
        assertEquals(List.of("connection-id"), created.get("followerUserIds"));
        assertEquals(1, created.get("followerCount"));
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
        assertEquals(List.of(), created.get("followerUserIds"));
    }

    @Test
    void createBannerGroupIsUniquePerOwner() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        ConnectionGroupEntity existingBanner = new ConnectionGroupEntity();
        existingBanner.setGroupType("BANNER");
        when(groups.findAllByOwnerIdOrderByCreatedAtDesc("owner-id"))
                .thenReturn(List.of(), List.of(existingBanner));
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConnectionGroupService service = service(groups, accounts, connections);
        Map<String, Object> created = service.createBannerGroup("owner@example.com");

        assertEquals("Banner", created.get("name"));
        assertEquals("BANNER", created.get("groupType"));
        assertEquals(true, created.get("isBanner"));
        assertEquals(true, created.get("isOwner"));
        assertThrows(BadRequestException.class, () -> service.createBannerGroup("owner@example.com"));
    }

    @Test
    void organizationPagesAreSharedAndOwnerCanCreateOnlyOne() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        owner.setName("Page owner");
        List<ConnectionGroupEntity> ownedPages = new java.util.ArrayList<>();
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findAllByOwnerIdOrderByCreatedAtDesc("owner-id")).thenReturn(ownedPages);
        when(groups.findAllByOrderByCreatedAtDesc()).thenAnswer(invocation -> {
            ConnectionGroupEntity savedPage = new ConnectionGroupEntity();
            savedPage.setId(12L);
            savedPage.setOwnerId("owner-id");
            savedPage.setOwnerName("Page owner");
            savedPage.setGroupType("ORG_PAGE");
            savedPage.setName("Example Page");
            savedPage.setPublicUrl("example-page");
            savedPage.setIndustry("Film");
            return List.of(savedPage);
        });
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> {
            ConnectionGroupEntity savedPage = invocation.getArgument(0);
            savedPage.setId(12L);
            ownedPages.add(savedPage);
            return savedPage;
        });
        ConnectionGroupService service = service(groups, accounts, connections);

        Map<String, Object> created = service.createOrganizationPage(
                "owner@example.com",
                "Example Page",
                "example-page",
                "https://example.com",
                "Film",
                "A sample page",
                "/uploads/logo.png",
                "/uploads/banner.png");

        assertEquals("ORG_PAGE", created.get("groupType"));
        assertEquals(true, created.get("isOwner"));
        assertEquals("example-page", created.get("publicUrl"));
        assertEquals("https://example.com", created.get("website"));
        assertEquals("/uploads/banner.png", created.get("bannerUrl"));
        List<Map<String, Object>> ownerPages = service.getOrganizationPages("owner@example.com");
        assertEquals(1, ownerPages.size());
        assertEquals(true, ownerPages.get(0).get("isOwner"));
        assertThrows(BadRequestException.class, () -> service.createOrganizationPage(
                "owner@example.com", "Another page", "another-page", "", "Film", "", "", ""));
    }

    @Test
    void organizationPagePostsAreVisibleToOtherAccounts() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity viewer = account(2L, "viewer@example.com", "viewer-id");
        ConnectionGroupEntity page = new ConnectionGroupEntity();
        page.setOwnerId("owner-id");
        page.setGroupType("ORG_PAGE");
        page.setFollowerUserIds(List.of("other-follower-id"));
        ConnectionGroupPostEntity post = new ConnectionGroupPostEntity();
        post.setContent("Public page update");
        when(accounts.findByEmail("viewer@example.com")).thenReturn(Optional.of(viewer));
        when(groups.findById(12L)).thenReturn(Optional.of(page));
        when(groups.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(page));
        when(posts.findAllByGroupIdOrderByCreatedAtDesc(12L)).thenReturn(List.of(post));

        ConnectionGroupService service = new ConnectionGroupService(groups, posts, accounts, connections);
        Map<String, Object> feed = service.getGroupPosts("viewer@example.com", 12L);
        Map<String, Object> publicPage = service.getOrganizationPages("viewer@example.com").get(0);

        assertEquals(true, feed.get("canViewAll"));
        assertEquals("Public page update", ((Map<?, ?>) ((List<?>) feed.get("posts")).get(0)).get("content"));
        assertEquals(false, publicPage.get("isOwner"));
        assertNull(publicPage.get("ownerId"));
        assertEquals(List.of(), publicPage.get("followerUserIds"));
    }

    @Test
    void blockedPageFollowerCannotSeePageOrPostsAndKeepsTheirRelationships() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionGroupEntity page = new ConnectionGroupEntity();
        page.setId(12L);
        page.setOwnerId("owner-id");
        page.setGroupType("ORG_PAGE");
        page.setFollowerUserIds(List.of("member-id"));
        page.setSubscriberUserIds(List.of("member-id"));
        when(accounts.findByEmail("owner@example.com"))
                .thenReturn(Optional.of(account(1L, "owner@example.com", "owner-id")));
        when(accounts.findByEmail("member@example.com"))
                .thenReturn(Optional.of(account(2L, "member@example.com", "member-id")));
        when(groups.findByIdAndOwnerId(12L, "owner-id")).thenReturn(Optional.of(page));
        when(groups.findById(12L)).thenReturn(Optional.of(page));
        when(groups.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(page));
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ConnectionGroupService service = new ConnectionGroupService(groups, posts, accounts, mock(ConnectionService.class));

        Map<String, Object> blocked = service.blockOrganizationPageMember(
                "owner@example.com", 12L, "member-id");

        assertEquals(List.of("member-id"), blocked.get("blockedUserIds"));
        assertEquals(List.of("member-id"), page.getFollowerUserIds());
        assertEquals(List.of("member-id"), page.getSubscriberUserIds());
        assertEquals(List.of(), service.getOrganizationPages("member@example.com"));
        Map<String, Object> feed = service.getGroupPosts("member@example.com", 12L);
        assertEquals(false, feed.get("canViewAll"));
        assertEquals(List.of(), feed.get("posts"));
        verify(posts, never()).findAllByGroupIdOrderByCreatedAtDesc(12L);

        service.unblockOrganizationPageMember("owner@example.com", 12L, "member-id");

        assertEquals(1, service.getOrganizationPages("member@example.com").size());
        assertEquals(List.of("member-id"), page.getFollowerUserIds());
        assertEquals(List.of("member-id"), page.getSubscriberUserIds());
    }

    @Test
    void pageOwnerCanOnlyBlockCurrentFollowersOrSubscribers() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionGroupEntity page = new ConnectionGroupEntity();
        page.setId(12L);
        page.setOwnerId("owner-id");
        page.setGroupType("ORG_PAGE");
        when(accounts.findByEmail("owner@example.com"))
                .thenReturn(Optional.of(account(1L, "owner@example.com", "owner-id")));
        when(groups.findByIdAndOwnerId(12L, "owner-id")).thenReturn(Optional.of(page));
        ConnectionGroupService service = new ConnectionGroupService(
                groups, mock(ConnectionGroupPostRepository.class), accounts, mock(ConnectionService.class));

        assertThrows(
                BadRequestException.class,
                () -> service.blockOrganizationPageMember("owner@example.com", 12L, "stranger-id"));
        verify(groups, never()).save(any(ConnectionGroupEntity.class));
    }

    @Test
    void pageOwnerCanRemoveFollowerWithoutRemovingTheirSubscription() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        ConnectionGroupEntity page = new ConnectionGroupEntity();
        page.setId(12L);
        page.setOwnerId("owner-id");
        page.setGroupType("ORG_PAGE");
        page.setFollowerUserIds(List.of("follower-id"));
        page.setSubscriberUserIds(List.of("follower-id"));
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findByIdAndOwnerId(12L, "owner-id")).thenReturn(Optional.of(page));
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ConnectionGroupService service = new ConnectionGroupService(
                groups, mock(ConnectionGroupPostRepository.class), accounts, mock(ConnectionService.class));

        Map<String, Object> updatedPage =
                service.removeOrganizationPageFollower("owner@example.com", 12L, "FOLLOWER-ID");

        assertEquals(List.of(), page.getFollowerUserIds());
        assertEquals(List.of("follower-id"), page.getSubscriberUserIds());
        assertEquals(0, updatedPage.get("followerCount"));
        assertEquals(List.of(), updatedPage.get("followerUserIds"));
        verify(groups).save(page);
        assertThrows(
                BadRequestException.class,
                () -> service.removeOrganizationPageFollower("owner@example.com", 12L, "follower-id"));
    }

    @Test
    void organizationPageFollowIsRecordedOnlyWhenFollowerIsAdded() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        OrganizationPageAnalyticsEventRepository analyticsEvents =
                mock(OrganizationPageAnalyticsEventRepository.class);
        UserAccountEntity follower = account(2L, "follower@example.com", "follower-id");
        ConnectionGroupEntity page = new ConnectionGroupEntity();
        page.setId(12L);
        page.setOwnerId("owner-id");
        page.setGroupType("ORG_PAGE");
        when(accounts.findByEmail("follower@example.com")).thenReturn(Optional.of(follower));
        when(groups.findById(12L)).thenReturn(Optional.of(page));
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(analyticsEvents.save(any(OrganizationPageAnalyticsEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ConnectionGroupService service = new ConnectionGroupService(
                groups,
                mock(ConnectionGroupPostRepository.class),
                mock(ConnectionGroupPostActivityRepository.class),
                null,
                null,
                accounts,
                mock(ConnectionService.class),
                null,
                null,
                analyticsEvents);

        service.followGroup("follower@example.com", 12L);
        service.followGroup("follower@example.com", 12L);

        assertEquals(List.of("follower-id"), page.getFollowerUserIds());
        verify(analyticsEvents, times(1)).save(any(OrganizationPageAnalyticsEventEntity.class));
    }

    @Test
    void organizationPageUnfollowRemovesFollowerAnalyticsEvent() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        OrganizationPageAnalyticsEventRepository analyticsEvents =
                mock(OrganizationPageAnalyticsEventRepository.class);
        UserAccountEntity follower = account(2L, "follower@example.com", "follower-id");
        ConnectionGroupEntity page = organizationPage(12L, "owner-id", "Example Page");
        page.setFollowerUserIds(List.of("follower-id"));
        when(accounts.findByEmail("follower@example.com")).thenReturn(Optional.of(follower));
        when(groups.findById(12L)).thenReturn(Optional.of(page));
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ConnectionGroupService service = new ConnectionGroupService(
                groups,
                mock(ConnectionGroupPostRepository.class),
                mock(ConnectionGroupPostActivityRepository.class),
                null,
                null,
                accounts,
                mock(ConnectionService.class),
                null,
                null,
                analyticsEvents);

        service.unfollowGroup("follower@example.com", 12L);

        assertEquals(List.of(), page.getFollowerUserIds());
        verify(analyticsEvents).deleteAllByGroupIdAndActorIdAndActivityType(12L, "follower-id", "FOLLOW");
    }

    @Test
    void pagePostRepliesAreReturnedAndCanBeDeletedByTheirAuthor() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        ConnectionGroupPostActivityRepository activities = mock(ConnectionGroupPostActivityRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserAccountEntity follower = account(2L, "follower@example.com", "follower-id");
        ConnectionGroupEntity page = organizationPage(7L, "owner-id", "Example Page");
        ConnectionGroupPostEntity post = new ConnectionGroupPostEntity();
        ReflectionTestUtils.setField(post, "id", 12L);
        post.setGroupId(7L);
        post.setAuthorId("owner-id");
        ConnectionGroupPostActivityEntity comment = new ConnectionGroupPostActivityEntity();
        ReflectionTestUtils.setField(comment, "id", 50L);
        comment.setGroupId(7L);
        comment.setPostId(12L);
        comment.setActorId("commenter-id");
        comment.setActorName("Commenter");
        comment.setActivityType("COMMENT");
        comment.setContent("Original comment");
        List<ConnectionGroupPostActivityEntity> savedActivities = new ArrayList<>(List.of(comment));

        when(accounts.findByEmail("follower@example.com")).thenReturn(Optional.of(follower));
        when(groups.findById(7L)).thenReturn(Optional.of(page));
        when(posts.findByIdAndGroupId(12L, 7L)).thenReturn(Optional.of(post));
        when(activities.findAllByPostIdOrderByCreatedAtAsc(12L))
                .thenAnswer(invocation -> new ArrayList<>(savedActivities));
        when(activities.save(any(ConnectionGroupPostActivityEntity.class))).thenAnswer(invocation -> {
            ConnectionGroupPostActivityEntity reply = invocation.getArgument(0);
            ReflectionTestUtils.setField(reply, "id", 51L);
            savedActivities.add(reply);
            return reply;
        });
        doAnswer(invocation -> {
            savedActivities.remove(invocation.getArgument(0));
            return null;
        }).when(activities).delete(any(ConnectionGroupPostActivityEntity.class));
        ConnectionGroupService service = new ConnectionGroupService(
                groups,
                posts,
                activities,
                accounts,
                mock(ConnectionService.class),
                null,
                null);

        assertThrows(
                BadRequestException.class,
                () -> service.deleteGroupPostActivity("follower@example.com", 7L, 12L, 50L));
        verify(activities, never()).delete(comment);

        Map<String, Object> withReply = service.recordGroupPostActivity(
                "follower@example.com", 7L, 12L, "COMMENT_REPLY", "A reply", 50L);
        ConnectionGroupPostActivityEntity replyActivity = savedActivities.get(1);
        Map<?, ?> returnedComment = (Map<?, ?>) ((List<?>) withReply.get("comments")).get(0);
        Map<?, ?> returnedReply = (Map<?, ?>) ((List<?>) returnedComment.get("replies")).get(0);
        assertEquals("A reply", returnedReply.get("content"));
        assertEquals(true, returnedReply.get("canDelete"));

        Map<String, Object> afterDelete =
                service.deleteGroupPostActivity("follower@example.com", 7L, 12L, 51L);

        Map<?, ?> remainingComment = (Map<?, ?>) ((List<?>) afterDelete.get("comments")).get(0);
        assertEquals(List.of(), remainingComment.get("replies"));
        verify(activities).delete(replyActivity);
    }

    @Test
    void bannerAnalyticsCountsRecentFollowsAndUniqueVisitors() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        OrganizationPageAnalyticsEventRepository analyticsEvents =
                mock(OrganizationPageAnalyticsEventRepository.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        ConnectionGroupEntity page = new ConnectionGroupEntity();
        page.setId(12L);
        page.setOwnerId("owner-id");
        page.setGroupType("ORG_PAGE");
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findById(12L)).thenReturn(Optional.of(page));
        when(analyticsEvents.findAllByGroupIdAndCreatedAtAfterOrderByCreatedAtAsc(
                eq(12L), any())).thenReturn(List.of(
                        analyticsEvent(12L, "FOLLOW", "follower-1"),
                        analyticsEvent(12L, "FOLLOW", "follower-2"),
                        analyticsEvent(12L, "VISIT", "visitor-1"),
                        analyticsEvent(12L, "VISIT", "visitor-1"),
                        analyticsEvent(12L, "VISIT", "visitor-2")));
        ConnectionGroupService service = new ConnectionGroupService(
                groups,
                mock(ConnectionGroupPostRepository.class),
                mock(ConnectionGroupPostActivityRepository.class),
                null,
                null,
                accounts,
                mock(ConnectionService.class),
                null,
                null,
                analyticsEvents);

        Map<String, Object> analytics = service.getOrganizationPageAnalytics("owner@example.com", 12L);

        assertEquals(2L, analytics.get("newFollowers"));
        assertEquals(2L, analytics.get("bannerVisitors"));
    }

    @Test
    void deactivatingOrganizationPageDeletesThePageAndItsPosts() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        ConnectionGroupEntity page = new ConnectionGroupEntity();
        page.setId(12L);
        page.setOwnerId("owner-id");
        page.setGroupType("ORG_PAGE");
        ConnectionGroupEntity duplicatePage = new ConnectionGroupEntity();
        duplicatePage.setId(13L);
        duplicatePage.setOwnerId("owner-id");
        duplicatePage.setGroupType("ORG_PAGE");
        ConnectionGroupEntity regularGroup = new ConnectionGroupEntity();
        regularGroup.setId(14L);
        regularGroup.setOwnerId("owner-id");
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findByIdAndOwnerId(12L, "owner-id")).thenReturn(Optional.of(page));
        when(groups.findAllByOwnerIdOrderByCreatedAtDesc("owner-id"))
                .thenReturn(List.of(page, duplicatePage, regularGroup));

        new ConnectionGroupService(groups, posts, accounts, mock(ConnectionService.class))
                .deleteOrganizationPage("owner@example.com", 12L);

        verify(posts).deleteAllByGroupId(12L);
        verify(posts).deleteAllByGroupId(13L);
        verify(groups).delete(page);
        verify(groups).delete(duplicatePage);
        verify(groups, never()).delete(regularGroup);
    }

    @Test
    void organizationPageListingReturnsOnlyTheNewestPagePerOwner() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupEntity newestPage = organizationPage(12L, "owner-id", "New page");
        ConnectionGroupEntity duplicatePage = organizationPage(11L, "owner-id", "Old page");
        ConnectionGroupEntity otherOwnerPage = organizationPage(10L, "other-owner-id", "Other page");
        when(groups.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(newestPage, duplicatePage, otherOwnerPage));

        List<Map<String, Object>> returnedPages = service(
                groups,
                mock(UserAccountRepository.class),
                mock(ConnectionService.class))
                .getOrganizationPages("viewer@example.com");

        assertEquals(List.of(12L, 10L), returnedPages.stream().map(page -> page.get("id")).toList());
    }

    @Test
    void bannerGroupsAreNotListedAsRegularGroups() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        ConnectionGroupEntity regularGroup = new ConnectionGroupEntity();
        regularGroup.setName("Film crew");
        ConnectionGroupEntity bannerGroup = new ConnectionGroupEntity();
        bannerGroup.setName("Banner");
        bannerGroup.setGroupType("BANNER");
        when(groups.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(regularGroup, bannerGroup));

        ConnectionGroupService service = service(groups, accounts, connections);

        assertEquals(1, service.getGroups("owner@example.com").size());
        assertEquals(false, ((Map<?, ?>) service.getGroups("owner@example.com").get(0)).get("isBanner"));
        assertEquals(1, service.getBannerGroups("owner@example.com").size());
        assertEquals(true, ((Map<?, ?>) service.getBannerGroups("owner@example.com").get(0)).get("isBanner"));
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
    void groupOwnerCanAddConnectionsAndTheirFollowers() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setId(7L);
        group.setOwnerId("owner-id");
        group.setName("Film crew");
        group.setMemberProfileIds(List.of("existing-member", "connected-id"));
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findByIdAndOwnerId(7L, "owner-id")).thenReturn(Optional.of(group));
        when(connections.getConnectedProfiles("owner@example.com")).thenReturn(List.of(
                Map.of("userId", "connected-id", "email", "connected@example.com")));
        when(connections.getFollowers("owner@example.com")).thenReturn(List.of(
                Map.of("userId", "follower-id", "email", "follower@example.com")));
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> updated = service(groups, accounts, connections)
                .addMembers("owner@example.com", 7L, List.of("connected@example.com", "follower-id"));

        assertEquals(List.of("existing-member"), updated.get("memberProfileIds"));
        assertEquals(List.of("connected-id", "follower-id"), updated.get("followerUserIds"));
        assertEquals(2, updated.get("followerCount"));
        verify(groups).save(group);
    }

    @Test
    void onlyGroupOwnerCanAddMembers() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity otherUser = account(2L, "other@example.com", "other-id");
        when(accounts.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        assertThrows(
                BadRequestException.class,
                () -> service(groups, accounts, connections)
                        .addMembers("other@example.com", 7L, List.of("connection-id")));
        verify(groups, never()).save(any(ConnectionGroupEntity.class));
    }

    @Test
    void groupOwnerCannotAddSomeoneWhoIsNeitherAConnectionNorAFollower() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setId(7L);
        group.setOwnerId("owner-id");
        group.setName("Film crew");
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findByIdAndOwnerId(7L, "owner-id")).thenReturn(Optional.of(group));
        when(connections.getConnectedProfiles("owner@example.com")).thenReturn(List.of());
        when(connections.getFollowers("owner@example.com")).thenReturn(List.of());

        assertThrows(
                BadRequestException.class,
                () -> service(groups, accounts, connections)
                        .addMembers("owner@example.com", 7L, List.of("unrelated-user")));
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
    void followingIsFollowerOnlyAndCanBeRemoved() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity viewer = account(2L, "viewer@example.com", "viewer-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setOwnerId("owner-id");
        group.setName("Film crew");
        group.setMemberProfileIds(List.of("member-id", "viewer-id"));
        when(accounts.findByEmail("viewer@example.com")).thenReturn(Optional.of(viewer));
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConnectionGroupService service = service(groups, accounts, connections);
        Map<String, Object> followed = service.followGroup("viewer@example.com", 7L);
        Map<String, Object> unfollowed = service.unfollowGroup("viewer@example.com", 7L);

        assertEquals(true, followed.get("isFollowing"));
        assertEquals(1, followed.get("followerCount"));
        assertEquals(List.of("member-id"), followed.get("memberProfileIds"));
        assertEquals(false, unfollowed.get("isFollowing"));
        assertEquals(0, unfollowed.get("followerCount"));
        assertEquals(List.of("member-id"), unfollowed.get("memberProfileIds"));
    }

    @Test
    void groupEditsDoNotTurnFollowersIntoMembers() {
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

        assertEquals(List.of("old-member"), updated.get("memberProfileIds"));
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
    void bannerMediaIsHiddenFromNonFollowersAndCannotContainText() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity viewer = account(2L, "viewer@example.com", "viewer-id");
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        ConnectionGroupEntity banner = new ConnectionGroupEntity();
        banner.setOwnerId("owner-id");
        banner.setGroupType("BANNER");
        ConnectionGroupPostEntity image = new ConnectionGroupPostEntity();
        image.setMediaUrl("/uploads/banner.jpg");
        image.setMediaType("image/jpeg");
        when(accounts.findByEmail("viewer@example.com")).thenReturn(Optional.of(viewer));
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findById(7L)).thenReturn(Optional.of(banner));
        when(groups.findByIdAndOwnerId(7L, "owner-id")).thenReturn(Optional.of(banner));
        when(groups.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(banner));
        when(posts.findAllByGroupIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(image));
        when(posts.save(any(ConnectionGroupPostEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConnectionGroupService service = new ConnectionGroupService(groups, posts, accounts, connections);
        Map<String, Object> feed = service.getGroupPosts("viewer@example.com", 7L);
        Map<String, Object> bannerDto = service.getBannerGroups("viewer@example.com").get(0);

        assertEquals(false, feed.get("canViewAll"));
        assertNull(feed.get("preview"));
        assertEquals(List.of(), bannerDto.get("followerUserIds"));
        assertThrows(
                BadRequestException.class,
                () -> service.createGroupPost(
                        "owner@example.com", 7L, "caption", "/uploads/banner.jpg", "image/jpeg"));
        Map<String, Object> createdPost = service.createGroupPost(
                "owner@example.com", 7L, "", "/uploads/banner.mp4", "video/mp4");
        assertEquals("/uploads/banner.mp4", createdPost.get("mediaUrl"));
        assertEquals("", createdPost.get("content"));
        verify(posts).save(any(ConnectionGroupPostEntity.class));
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
        when(groups.findById(7L)).thenReturn(Optional.of(group));
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
    void assignedPageAdminCanCreateOrganizationPagePosts() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity admin = account(2L, "admin@example.com", "admin-id");
        ConnectionGroupEntity page = new ConnectionGroupEntity();
        page.setId(7L);
        page.setOwnerId("owner-id");
        page.setGroupType("ORG_PAGE");
        page.setPageAdminUserIds(List.of("admin-id"));
        when(accounts.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(groups.findById(7L)).thenReturn(Optional.of(page));
        when(posts.save(any(ConnectionGroupPostEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ConnectionGroupService service = new ConnectionGroupService(groups, posts, accounts, connections);

        Map<String, Object> created =
                service.createGroupPost("admin@example.com", 7L, "Admin post", null, null);

        assertEquals("admin-id", created.get("authorId"));
        assertEquals(true, created.get("canEdit"));
        assertEquals("Admin post", created.get("content"));
        verify(posts).save(any(ConnectionGroupPostEntity.class));
    }

    @Test
    void pageOwnerCanAddAndRemoveAnAdminFromCurrentConnections() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        ConnectionGroupEntity page = new ConnectionGroupEntity();
        page.setId(7L);
        page.setOwnerId("owner-id");
        page.setGroupType("ORG_PAGE");
        page.setOwnerName("Owner");
        List<Map<String, Object>> connectionProfiles = List.of(
                Map.of("userId", "connection-id", "id", "connection-id", "name", "Connection"));
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findByIdAndOwnerId(7L, "owner-id")).thenReturn(Optional.of(page));
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(connections.getConnectedProfiles("owner@example.com")).thenReturn(connectionProfiles);
        when(connections.getConnectedProfiles("owner-id")).thenReturn(connectionProfiles);
        ConnectionGroupService service = new ConnectionGroupService(groups, posts, accounts, connections);

        Map<String, Object> afterAdd =
                service.addOrganizationPageAdmin("owner@example.com", 7L, "connection-id");
        List<?> addedAdmins = (List<?>) afterAdd.get("admins");
        assertEquals(List.of("connection-id"), page.getPageAdminUserIds());
        assertEquals(2, addedAdmins.size());
        assertEquals("Connection", ((Map<?, ?>) addedAdmins.get(1)).get("name"));

        Map<String, Object> afterRemove =
                service.removeOrganizationPageAdmin("owner@example.com", 7L, "connection-id");
        assertEquals(List.of(), page.getPageAdminUserIds());
        assertEquals(1, ((List<?>) afterRemove.get("admins")).size());
    }

    @Test
    void pageOwnerCannotAddSomeoneWhoIsNotAConnection() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        ConnectionGroupEntity page = new ConnectionGroupEntity();
        page.setId(7L);
        page.setOwnerId("owner-id");
        page.setGroupType("ORG_PAGE");
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findByIdAndOwnerId(7L, "owner-id")).thenReturn(Optional.of(page));
        when(connections.getConnectedProfiles("owner@example.com")).thenReturn(List.of());
        ConnectionGroupService service = new ConnectionGroupService(groups, posts, accounts, connections);

        assertThrows(
                BadRequestException.class,
                () -> service.addOrganizationPageAdmin("owner@example.com", 7L, "unconnected-id"));
        verify(groups, never()).save(any(ConnectionGroupEntity.class));
    }

    @Test
    void assignedPageAdminCanAccessTheCreatorDashboard() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity admin = account(2L, "admin@example.com", "admin-id");
        ConnectionGroupEntity page = new ConnectionGroupEntity();
        page.setId(7L);
        page.setOwnerId("owner-id");
        page.setGroupType("ORG_PAGE");
        page.setPageAdminUserIds(List.of("admin-id"));
        when(accounts.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(groups.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(page));

        Map<String, Object> pageDto = new ConnectionGroupService(groups, posts, accounts, connections)
                .getOrganizationPages("admin@example.com")
                .get(0);

        assertEquals(true, pageDto.get("isAdmin"));
        assertEquals(false, pageDto.get("isOwner"));
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
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        when(posts.findByIdAndGroupId(12L, 7L)).thenReturn(Optional.of(post));

        new ConnectionGroupService(groups, posts, accounts, connections)
                .deleteGroupPost("owner@example.com", 7L, 12L);

        verify(posts).delete(post);
    }

    @Test
    void postAuthorCanEditTheirOwnUpdate() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity author = account(2L, "author@example.com", "author-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setId(7L);
        group.setOwnerId("owner-id");
        ConnectionGroupPostEntity post = new ConnectionGroupPostEntity();
        post.setGroupId(7L);
        post.setAuthorId("author-id");
        post.setAuthorName("Author");
        when(accounts.findByEmail("author@example.com")).thenReturn(Optional.of(author));
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        when(posts.findByIdAndGroupId(12L, 7L)).thenReturn(Optional.of(post));
        when(posts.save(any(ConnectionGroupPostEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> updated = new ConnectionGroupService(groups, posts, accounts, connections)
                .updateGroupPost("author@example.com", 7L, 12L, "Updated text", null, null);

        assertEquals("Updated text", post.getContent());
        assertEquals(true, updated.get("canEdit"));
        verify(posts).save(post);
    }

    @Test
    void assignedPageAdminCanEditAndDeleteAnyOrganizationPagePost() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity admin = account(2L, "admin@example.com", "admin-id");
        ConnectionGroupEntity page = new ConnectionGroupEntity();
        page.setId(7L);
        page.setOwnerId("owner-id");
        page.setGroupType("ORG_PAGE");
        page.setPageAdminUserIds(List.of("admin-id"));
        ConnectionGroupPostEntity post = new ConnectionGroupPostEntity();
        post.setGroupId(7L);
        post.setAuthorId("author-id");
        post.setAuthorName("Author");
        when(accounts.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(groups.findById(7L)).thenReturn(Optional.of(page));
        when(posts.findByIdAndGroupId(12L, 7L)).thenReturn(Optional.of(post));
        when(posts.save(any(ConnectionGroupPostEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ConnectionGroupService service = new ConnectionGroupService(groups, posts, accounts, connections);

        Map<String, Object> updated =
                service.updateGroupPost("admin@example.com", 7L, 12L, "Updated by admin", null, null);
        service.deleteGroupPost("admin@example.com", 7L, 12L);

        assertEquals("Updated by admin", post.getContent());
        assertEquals(true, updated.get("canEdit"));
        verify(posts).save(post);
        verify(posts).delete(post);
    }

    @Test
    void anotherUserCannotEditOrDeleteAnUpdate() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        UserAccountEntity other = account(3L, "other@example.com", "other-id");
        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setId(7L);
        ConnectionGroupPostEntity post = new ConnectionGroupPostEntity();
        post.setAuthorId("author-id");
        when(accounts.findByEmail("other@example.com")).thenReturn(Optional.of(other));
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        when(posts.findByIdAndGroupId(12L, 7L)).thenReturn(Optional.of(post));
        ConnectionGroupService service = new ConnectionGroupService(groups, posts, accounts, connections);

        assertThrows(
                BadRequestException.class,
                () -> service.updateGroupPost("other@example.com", 7L, 12L, "Changed", null, null));
        assertThrows(
                BadRequestException.class,
                () -> service.deleteGroupPost("other@example.com", 7L, 12L));
        verify(posts, never()).save(any(ConnectionGroupPostEntity.class));
        verify(posts, never()).delete(any(ConnectionGroupPostEntity.class));
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
        when(groups.findById(7L)).thenReturn(Optional.of(group));
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

    @Test
    void getGroupsAndCreateGroupSucceedEvenWhenUserNotYetInDatabase() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        ConnectionService connections = mock(ConnectionService.class);
        when(accounts.findByEmail("aabbb@gmail.com")).thenReturn(Optional.empty());
        when(accounts.findByPublicUserId("aabbb@gmail.com")).thenReturn(Optional.empty());
        when(groups.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConnectionGroupService service = service(groups, accounts, connections);
        List<Map<String, Object>> returnedGroups = service.getGroups("aabbb@gmail.com");
        assertEquals(0, returnedGroups.size());

        Map<String, Object> created = service.createGroup("aabbb@gmail.com", "xx", List.of(), null);
        assertEquals("xx", created.get("name"));
        assertEquals("aabbb@gmail.com", created.get("ownerId"));
        assertEquals(true, created.get("isOwner"));
    }

    @Test
    void followerPostActivitiesAreReturnedForAnalytics() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        ConnectionGroupPostRepository posts = mock(ConnectionGroupPostRepository.class);
        ConnectionGroupPostActivityRepository activities = mock(ConnectionGroupPostActivityRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserAccountEntity follower = account(2L, "follower@example.com", "follower-id");
        ConnectionGroupEntity page = organizationPage(7L, "owner-id", "Example Page");
        ConnectionGroupPostEntity post = new ConnectionGroupPostEntity();
        ReflectionTestUtils.setField(post, "id", 12L);
        post.setGroupId(7L);
        post.setContent("Page update");
        List<ConnectionGroupPostActivityEntity> savedActivities = new ArrayList<>();

        when(accounts.findByEmail("follower@example.com")).thenReturn(Optional.of(follower));
        when(groups.findById(7L)).thenReturn(Optional.of(page));
        when(posts.findByIdAndGroupId(12L, 7L)).thenReturn(Optional.of(post));
        when(activities.findAllByPostIdOrderByCreatedAtAsc(12L))
                .thenAnswer(invocation -> new ArrayList<>(savedActivities));
        when(activities.save(any(ConnectionGroupPostActivityEntity.class))).thenAnswer(invocation -> {
            ConnectionGroupPostActivityEntity activity = invocation.getArgument(0);
            savedActivities.add(activity);
            return activity;
        });

        ConnectionGroupService service = new ConnectionGroupService(
                groups,
                posts,
                activities,
                accounts,
                mock(ConnectionService.class),
                null,
                null);

        Map<String, Object> reactedPost = service.recordGroupPostActivity(
                "follower@example.com", 7L, 12L, "REACTION", null);
        assertEquals(true, reactedPost.get("viewerReacted"));
        assertEquals(1, ((List<?>) reactedPost.get("reactions")).size());

        Map<String, Object> commentedPost = service.recordGroupPostActivity(
                "follower@example.com", 7L, 12L, "COMMENT", "Thanks for sharing");
        List<?> comments = (List<?>) commentedPost.get("comments");
        assertEquals(1, comments.size());
        assertEquals("Thanks for sharing", ((Map<?, ?>) comments.get(0)).get("content"));
    }

    @Test
    void pageSubscriptionRequestNotifiesOwnerAndAcceptanceAddsSubscriber() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        OrganizationPageSubscriptionRequestRepository requests =
                mock(OrganizationPageSubscriptionRequestRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        owner.setName("Page Owner");
        UserAccountEntity requester = account(2L, "member@example.com", "member-id");
        requester.setName("Page Member");
        ConnectionGroupEntity page = organizationPage(7L, "owner-id", "Example Page");
        OrganizationPageSubscriptionRequestEntity request = new OrganizationPageSubscriptionRequestEntity();
        request.setPageId(7L);
        request.setRequesterId("member-id");
        request.setRequesterName("Page Member");
        ReflectionTestUtils.setField(request, "id", 21L);

        when(accounts.findByEmail("member@example.com")).thenReturn(Optional.of(requester));
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(groups.findById(7L)).thenReturn(Optional.of(page));
        when(groups.findByIdAndOwnerId(7L, "owner-id")).thenReturn(Optional.of(page));
        when(requests.findFirstByPageIdAndRequesterIdAndStatusIgnoreCaseOrderByCreatedAtDesc(
                7L, "member-id", "PENDING")).thenReturn(Optional.empty());
        when(requests.existsByPageIdAndRequesterIdInAndStatusIgnoreCase(
                org.mockito.ArgumentMatchers.eq(7L), any(), org.mockito.ArgumentMatchers.eq("PENDING")))
                .thenReturn(true);
        when(requests.save(any(OrganizationPageSubscriptionRequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(requests.findById(21L)).thenReturn(Optional.of(request));
        when(groups.save(any(ConnectionGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConnectionGroupService service = new ConnectionGroupService(
                groups,
                mock(ConnectionGroupPostRepository.class),
                null,
                requests,
                notifications,
                accounts,
                mock(ConnectionService.class),
                null,
                null);

        Map<String, Object> pageState = service.requestOrganizationPageSubscription("member@example.com", 7L);
        assertEquals(true, pageState.get("isSubscriptionPending"));
        verify(notifications).createNotification(
                org.mockito.ArgumentMatchers.eq("owner-id"),
                org.mockito.ArgumentMatchers.eq("New page subscription request"),
                org.mockito.ArgumentMatchers.contains("Page Member"),
                org.mockito.ArgumentMatchers.eq("connection_page_subscription_request"));

        Map<String, Object> decision = service.respondToOrganizationPageSubscriptionRequest(
                "owner@example.com", 21L, "ACCEPT");
        assertEquals("ACCEPTED", decision.get("status"));
        assertEquals(List.of("member-id"), page.getSubscriberUserIds());
        verify(notifications).createNotification(
                org.mockito.ArgumentMatchers.eq("member-id"),
                org.mockito.ArgumentMatchers.eq("Subscription request accepted"),
                org.mockito.ArgumentMatchers.contains("Page Member"),
                org.mockito.ArgumentMatchers.eq("connection_page_subscription_accepted"));
    }

    @Test
    void rejectingPageSubscriptionNotifiesRequesterByNameWithoutAddingSubscriber() {
        ConnectionGroupRepository groups = mock(ConnectionGroupRepository.class);
        OrganizationPageSubscriptionRequestRepository requests =
                mock(OrganizationPageSubscriptionRequestRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        owner.setName("Page Owner");
        ConnectionGroupEntity page = organizationPage(7L, "owner-id", "Example Page");
        OrganizationPageSubscriptionRequestEntity request = new OrganizationPageSubscriptionRequestEntity();
        request.setPageId(7L);
        request.setRequesterId("member-id");
        request.setRequesterName("Page Member");
        ReflectionTestUtils.setField(request, "id", 22L);

        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(requests.findById(22L)).thenReturn(Optional.of(request));
        when(groups.findByIdAndOwnerId(7L, "owner-id")).thenReturn(Optional.of(page));

        ConnectionGroupService service = new ConnectionGroupService(
                groups,
                mock(ConnectionGroupPostRepository.class),
                null,
                requests,
                notifications,
                accounts,
                mock(ConnectionService.class),
                null,
                null);

        Map<String, Object> decision = service.respondToOrganizationPageSubscriptionRequest(
                "owner@example.com", 22L, "REJECT");

        assertEquals("REJECTED", decision.get("status"));
        assertEquals(List.of(), page.getSubscriberUserIds());
        verify(notifications).createNotification(
                org.mockito.ArgumentMatchers.eq("member-id"),
                org.mockito.ArgumentMatchers.eq("Subscription request rejected"),
                org.mockito.ArgumentMatchers.contains("Page Member"),
                org.mockito.ArgumentMatchers.eq("connection_page_subscription_rejected"));
    }

    private static UserAccountEntity account(Long id, String email, String publicUserId) {
        UserAccountEntity account = new UserAccountEntity();
        account.setId(id);
        account.setEmail(email);
        account.setPublicUserId(publicUserId);
        return account;
    }

    private static OrganizationPageAnalyticsEventEntity analyticsEvent(
            Long groupId,
            String activityType,
            String actorId) {
        OrganizationPageAnalyticsEventEntity event = new OrganizationPageAnalyticsEventEntity();
        event.setGroupId(groupId);
        event.setActivityType(activityType);
        event.setActorId(actorId);
        return event;
    }

    private static ConnectionGroupEntity organizationPage(Long id, String ownerId, String name) {
        ConnectionGroupEntity page = new ConnectionGroupEntity();
        page.setId(id);
        page.setOwnerId(ownerId);
        page.setGroupType("ORG_PAGE");
        page.setName(name);
        return page;
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
