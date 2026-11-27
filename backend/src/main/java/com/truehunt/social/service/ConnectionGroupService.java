package com.truehunt.social.service;

import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.model.ConnectionGroupEntity;
import com.truehunt.social.model.ConnectionGroupPostActivityEntity;
import com.truehunt.social.model.ConnectionGroupPostEntity;
import com.truehunt.social.model.OrganizationPageSubscriptionRequestEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ConnectionGroupRepository;
import com.truehunt.social.repository.ConnectionGroupPostActivityRepository;
import com.truehunt.social.repository.ConnectionGroupPostRepository;
import com.truehunt.social.repository.OrganizationPageSubscriptionRequestRepository;
import com.truehunt.social.repository.ConversationRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ConnectionGroupService {

    private static final int MAX_GROUP_NAME_LENGTH = 80;
    private static final int MAX_ORGANIZATION_PAGE_NAME_LENGTH = 100;

    private final ConnectionGroupRepository groupRepository;
    private final ConnectionGroupPostRepository groupPostRepository;
    private final ConnectionGroupPostActivityRepository postActivityRepository;
    private final OrganizationPageSubscriptionRequestRepository subscriptionRequestRepository;
    private final NotificationService notificationService;
    private final UserAccountRepository userAccountRepository;
    private final ConnectionService connectionService;
    private final ConversationRepository conversationRepository;
    private final ProfileRepository profileRepository;

    public ConnectionGroupService(
            ConnectionGroupRepository groupRepository,
            ConnectionGroupPostRepository groupPostRepository,
            UserAccountRepository userAccountRepository,
            ConnectionService connectionService) {
        this(groupRepository, groupPostRepository, null, null, null, userAccountRepository,
                connectionService, null, null);
    }

    public ConnectionGroupService(
            ConnectionGroupRepository groupRepository,
            ConnectionGroupPostRepository groupPostRepository,
            UserAccountRepository userAccountRepository,
            ConnectionService connectionService,
            ConversationRepository conversationRepository) {
        this(groupRepository, groupPostRepository, null, null, null, userAccountRepository,
                connectionService, conversationRepository, null);
    }

    public ConnectionGroupService(
            ConnectionGroupRepository groupRepository,
            ConnectionGroupPostRepository groupPostRepository,
            UserAccountRepository userAccountRepository,
            ConnectionService connectionService,
            ConversationRepository conversationRepository,
            ProfileRepository profileRepository) {
        this(groupRepository, groupPostRepository, null, null, null, userAccountRepository,
                connectionService, conversationRepository, profileRepository);
    }

    public ConnectionGroupService(
            ConnectionGroupRepository groupRepository,
            ConnectionGroupPostRepository groupPostRepository,
            ConnectionGroupPostActivityRepository postActivityRepository,
            UserAccountRepository userAccountRepository,
            ConnectionService connectionService,
            ConversationRepository conversationRepository,
            ProfileRepository profileRepository) {
        this(groupRepository, groupPostRepository, postActivityRepository, null, null, userAccountRepository,
                connectionService, conversationRepository, profileRepository);
    }

    @Autowired
    public ConnectionGroupService(
            ConnectionGroupRepository groupRepository,
            ConnectionGroupPostRepository groupPostRepository,
            ConnectionGroupPostActivityRepository postActivityRepository,
            OrganizationPageSubscriptionRequestRepository subscriptionRequestRepository,
            NotificationService notificationService,
            UserAccountRepository userAccountRepository,
            ConnectionService connectionService,
            ConversationRepository conversationRepository,
            ProfileRepository profileRepository) {
        this.groupRepository = groupRepository;
        this.groupPostRepository = groupPostRepository;
        this.postActivityRepository = postActivityRepository;
        this.subscriptionRequestRepository = subscriptionRequestRepository;
        this.notificationService = notificationService;
        this.userAccountRepository = userAccountRepository;
        this.connectionService = connectionService;
        this.conversationRepository = conversationRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGroups(String ownerIdentifier) {
        Set<String> viewerIdentities = resolveViewerIdentities(ownerIdentifier);
        return groupRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(group -> !isBannerGroup(group) && !isOrganizationPage(group))
                .map(group -> toDto(group, viewerIdentities))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBannerGroups(String ownerIdentifier) {
        Set<String> viewerIdentities = resolveViewerIdentities(ownerIdentifier);
        return groupRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(this::isBannerGroup)
                .map(group -> toDto(group, viewerIdentities))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOrganizationPages(String viewerIdentifier) {
        Set<String> viewerIdentities = resolveViewerIdentities(viewerIdentifier);
        Map<String, ConnectionGroupEntity> newestPageByOwner = new LinkedHashMap<>();
        for (ConnectionGroupEntity group : groupRepository.findAllByOrderByCreatedAtDesc()) {
            if (!isOrganizationPage(group)) continue;
            String ownerKey = normalize(group.getOwnerId());
            newestPageByOwner.putIfAbsent(ownerKey, group);
        }
        return newestPageByOwner.values().stream()
                .map(group -> toDto(group, viewerIdentities))
                .toList();
    }

    @Transactional
    public Map<String, Object> createOrganizationPage(
            String ownerIdentifier,
            String name,
            String publicUrl,
            String website,
            String industry,
            String tagline,
            String logo,
            String bannerUrl) {
        ResolvedOwner owner = resolveOwner(ownerIdentifier);
        List<ConnectionGroupEntity> ownedGroups = groupRepository.findAllByOwnerIdOrderByCreatedAtDesc(owner.id());
        if (ownedGroups != null && ownedGroups.stream().anyMatch(this::isOrganizationPage)) {
            throw new BadRequestException("You already have an organization page.");
        }

        ConnectionGroupEntity page = new ConnectionGroupEntity();
        page.setOwnerId(owner.id());
        page.setOwnerName(owner.name());
        page.setGroupType("ORG_PAGE");
        applyOrganizationPageFields(page, name, publicUrl, website, industry, tagline, logo, bannerUrl);
        page.setMemberProfileIds(new ArrayList<>());
        page.setFollowerUserIds(new ArrayList<>());
        page.setSubscriberUserIds(new ArrayList<>());
        return toDto(groupRepository.save(page), owner.id());
    }

    @Transactional
    public Map<String, Object> updateOrganizationPage(
            String ownerIdentifier,
            Long pageId,
            String name,
            String publicUrl,
            String website,
            String industry,
            String tagline,
            String logo,
            String bannerUrl) {
        String ownerId = resolveOwnerId(ownerIdentifier);
        ConnectionGroupEntity page = groupRepository.findByIdAndOwnerId(pageId, ownerId)
                .filter(this::isOrganizationPage)
                .orElseThrow(() -> new BadRequestException("Could not find this organization page."));
        applyOrganizationPageFields(page, name, publicUrl, website, industry, tagline, logo, bannerUrl);
        return toDto(groupRepository.save(page), ownerId);
    }

    @Transactional
    public void deleteOrganizationPage(String ownerIdentifier, Long pageId) {
        String ownerId = resolveOwnerId(ownerIdentifier);
        ConnectionGroupEntity page = groupRepository.findByIdAndOwnerId(pageId, ownerId)
                .filter(this::isOrganizationPage)
                .orElseThrow(() -> new BadRequestException("Could not find this organization page."));
        List<ConnectionGroupEntity> ownerPages = groupRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
        for (ConnectionGroupEntity ownerPage : ownerPages) {
            if (!isOrganizationPage(ownerPage)) continue;
            if (subscriptionRequestRepository != null) {
                subscriptionRequestRepository.deleteAllByPageIdIn(List.of(ownerPage.getId()));
            }
            if (postActivityRepository != null) {
                postActivityRepository.deleteAllByGroupId(ownerPage.getId());
            }
            groupPostRepository.deleteAllByGroupId(ownerPage.getId());
            groupRepository.delete(ownerPage);
        }
    }

    @Transactional
    public Map<String, Object> subscribeToOrganizationPage(String userIdentifier, Long pageId, boolean subscribe) {
        return subscribe
                ? requestOrganizationPageSubscription(userIdentifier, pageId)
                : unsubscribeFromOrganizationPage(userIdentifier, pageId);
    }

    @Transactional
    public Map<String, Object> requestOrganizationPageSubscription(String userIdentifier, Long pageId) {
        ResolvedOwner requester = resolveOwner(userIdentifier);
        ConnectionGroupEntity page = groupRepository.findById(pageId)
                .filter(this::isOrganizationPage)
                .orElseThrow(() -> new BadRequestException("Could not find this organization page."));
        if (normalize(page.getOwnerId()).equals(normalize(requester.id()))) {
            throw new BadRequestException("Page owners cannot subscribe to their own page.");
        }
        if (page.getSubscriberUserIds().stream()
                .anyMatch(id -> normalize(id).equals(normalize(requester.id())))) {
            return toDto(page, requester.id());
        }
        if (subscriptionRequestRepository == null || notificationService == null) {
            throw new IllegalStateException("Organization page subscription requests are not configured.");
        }
        if (subscriptionRequestRepository
                .findFirstByPageIdAndRequesterIdAndStatusIgnoreCaseOrderByCreatedAtDesc(
                        pageId, requester.id(), "PENDING")
                .isPresent()) {
            return toDto(page, requester.id());
        }

        OrganizationPageSubscriptionRequestEntity request = new OrganizationPageSubscriptionRequestEntity();
        request.setPageId(pageId);
        request.setRequesterId(requester.id());
        request.setRequesterName(displayName(requester.name(), "A member"));
        subscriptionRequestRepository.save(request);

        notificationService.createNotification(
                page.getOwnerId(),
                "New page subscription request",
                request.getRequesterName() + " requested to subscribe to " + page.getName() + ".",
                "connection_page_subscription_request");
        return toDto(page, requester.id());
    }

    @Transactional
    public Map<String, Object> unsubscribeFromOrganizationPage(String userIdentifier, Long pageId) {
        String userId = resolveOwnerId(userIdentifier);
        ConnectionGroupEntity page = groupRepository.findById(pageId)
                .filter(this::isOrganizationPage)
                .orElseThrow(() -> new BadRequestException("Could not find this organization page."));
        if (normalize(page.getOwnerId()).equals(normalize(userId))) {
            return toDto(page, userId);
        }
        page.getSubscriberUserIds().removeIf(id -> normalize(id).equals(normalize(userId)));
        groupRepository.save(page);
        if (subscriptionRequestRepository != null) {
            subscriptionRequestRepository
                    .findFirstByPageIdAndRequesterIdAndStatusIgnoreCaseOrderByCreatedAtDesc(
                            pageId, userId, "PENDING")
                    .ifPresent(subscriptionRequestRepository::delete);
        }
        return toDto(page, userId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOrganizationPageSubscriptionRequests(String ownerIdentifier) {
        ResolvedOwner owner = resolveOwner(ownerIdentifier);
        if (subscriptionRequestRepository == null) {
            throw new IllegalStateException("Organization page subscription requests are not configured.");
        }
        Map<Long, ConnectionGroupEntity> pagesById = new LinkedHashMap<>();
        groupRepository.findAllByOwnerIdOrderByCreatedAtDesc(owner.id()).stream()
                .filter(this::isOrganizationPage)
                .forEach(page -> pagesById.put(page.getId(), page));
        if (pagesById.isEmpty()) return List.of();

        return subscriptionRequestRepository
                .findAllByPageIdInAndStatusIgnoreCaseOrderByCreatedAtDesc(pagesById.keySet(), "PENDING")
                .stream()
                .map(request -> {
                    ConnectionGroupEntity page = pagesById.get(request.getPageId());
                    if (page == null) return null;
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("id", request.getId());
                    result.put("pageId", page.getId());
                    result.put("pageName", page.getName());
                    result.put("requesterId", request.getRequesterId());
                    result.put("requesterName", request.getRequesterName());
                    result.put("createdAt", request.getCreatedAt());
                    return result;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public Map<String, Object> respondToOrganizationPageSubscriptionRequest(
            String ownerIdentifier,
            Long requestId,
            String decision) {
        ResolvedOwner owner = resolveOwner(ownerIdentifier);
        if (subscriptionRequestRepository == null || notificationService == null) {
            throw new IllegalStateException("Organization page subscription requests are not configured.");
        }
        String normalizedDecision = decision == null ? "" : decision.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ACCEPT", "REJECT").contains(normalizedDecision)) {
            throw new BadRequestException("Choose whether to accept or reject this subscription request.");
        }
        OrganizationPageSubscriptionRequestEntity request = subscriptionRequestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Could not find this subscription request."));
        ConnectionGroupEntity page = groupRepository.findByIdAndOwnerId(request.getPageId(), owner.id())
                .filter(this::isOrganizationPage)
                .orElseThrow(() -> new BadRequestException("You cannot respond to this page subscription request."));
        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new BadRequestException("This subscription request has already been answered.");
        }

        boolean accepted = "ACCEPT".equals(normalizedDecision);
        request.setStatus(accepted ? "ACCEPTED" : "REJECTED");
        subscriptionRequestRepository.save(request);
        if (accepted && page.getSubscriberUserIds().stream()
                .noneMatch(id -> normalize(id).equals(normalize(request.getRequesterId())))) {
            page.getSubscriberUserIds().add(request.getRequesterId());
            groupRepository.save(page);
        }

        String ownerName = displayName(owner.name(), "The page owner");
        notificationService.createNotification(
                request.getRequesterId(),
                accepted ? "Subscription request accepted" : "Subscription request rejected",
                ownerName + (accepted ? " accepted" : " rejected") + " " + request.getRequesterName()
                        + "'s request to subscribe to " + page.getName() + ".",
                accepted
                        ? "connection_page_subscription_accepted"
                        : "connection_page_subscription_rejected");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", request.getId());
        result.put("status", request.getStatus());
        result.put("page", toDto(page, owner.id()));
        return result;
    }

    @Transactional
    public Map<String, Object> createBannerGroup(String ownerIdentifier) {
        String ownerId = resolveOwnerId(ownerIdentifier);
        List<ConnectionGroupEntity> ownedGroups = groupRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
        if (ownedGroups != null && ownedGroups.stream().anyMatch(this::isBannerGroup)) {
            throw new BadRequestException("You already have a banner group.");
        }

        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setOwnerId(ownerId);
        group.setName("Banner");
        group.setGroupType("BANNER");
        group.setMemberProfileIds(new ArrayList<>());
        group.setFollowerUserIds(new ArrayList<>());
        return toDto(groupRepository.save(group), ownerId);
    }

    @Transactional(readOnly = true)
    public ConnectionGroupEntity getGroupForMessaging(String ownerIdentifier, Long groupId) {
        if (groupId == null) {
            throw new BadRequestException("Group id is required.");
        }
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new BadRequestException("Could not find this group."));
    }

    @Transactional
    public Map<String, Object> createGroup(
            String ownerIdentifier,
            String name,
            List<String> requestedMemberIds,
            String imageUrl) {
        String ownerId = resolveOwnerId(ownerIdentifier);
        String groupName = name == null ? "" : name.trim();
        validateGroupName(groupName);
        Set<String> selectedMemberIds = resolveConnectedMemberIds(ownerIdentifier, requestedMemberIds, true);

        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setOwnerId(ownerId);
        group.setName(groupName);
        group.setImageUrl(normalizeImageUrl(imageUrl));
        group.setMemberProfileIds(new ArrayList<>());
        group.setFollowerUserIds(selectedMemberIds.stream()
                .filter(memberId -> !normalize(memberId).equals(normalize(ownerId)))
                .toList());
        return toDto(groupRepository.save(group), ownerId);
    }

    @Transactional
    public Map<String, Object> updateGroup(
            String ownerIdentifier,
            Long groupId,
            String name,
            List<String> requestedMemberIds,
            String imageUrl) {
        String groupName = name == null ? "" : name.trim();
        validateGroupName(groupName);
        if (requestedMemberIds == null) {
            throw new BadRequestException("Provide the group members to save.");
        }

        String ownerId = resolveOwnerId(ownerIdentifier);
        ConnectionGroupEntity group = groupRepository.findByIdAndOwnerId(groupId, ownerId)
                .orElseThrow(() -> new BadRequestException("Could not find this group."));
        Set<String> selectedMemberIds = new LinkedHashSet<>(group.getMemberProfileIds());
        selectedMemberIds.removeIf(memberId -> group.getFollowerUserIds().stream()
                .anyMatch(followerId -> normalize(followerId).equals(normalize(memberId))));
        group.setName(groupName);
        if (imageUrl != null) {
            group.setImageUrl(normalizeImageUrl(imageUrl));
        }
        group.setMemberProfileIds(new ArrayList<>(selectedMemberIds));
        ConnectionGroupEntity savedGroup = groupRepository.save(group);
        if (conversationRepository != null && savedGroup.getId() != null) {
            conversationRepository.findFirstByConnectionGroupId(savedGroup.getId())
                    .ifPresent(conversation -> {
                        conversation.setGroupImageUrl(savedGroup.getImageUrl());
                        conversationRepository.save(conversation);
                    });
        }
        return toDto(savedGroup, ownerId);
    }

    @Transactional
    public Map<String, Object> addMembers(
            String ownerIdentifier,
            Long groupId,
            List<String> requestedMemberIds) {
        if (requestedMemberIds == null || requestedMemberIds.isEmpty()) {
            throw new BadRequestException("Select at least one profile to add.");
        }

        String ownerId = resolveOwnerId(ownerIdentifier);
        ConnectionGroupEntity group = groupRepository.findByIdAndOwnerId(groupId, ownerId)
                .orElseThrow(() -> new BadRequestException("Only the group creator can add members."));

        Map<String, String> eligibleMemberIds = new LinkedHashMap<>();
        List<Map<String, Object>> eligibleProfiles = new ArrayList<>(
                connectionService.getConnectedProfiles(ownerIdentifier));
        eligibleProfiles.addAll(connectionService.getFollowers(ownerIdentifier));
        for (Map<String, Object> profile : eligibleProfiles) {
            if (profile == null) continue;
            String canonicalId = firstValue(profile, "userId", "publicUserId", "id", "profileId");
            if (canonicalId == null) continue;
            for (String identity : identityValues(profile)) {
                eligibleMemberIds.putIfAbsent(normalize(identity), canonicalId);
            }
        }

        Set<String> memberIds = new LinkedHashSet<>(group.getMemberProfileIds());
        Set<String> followerIds = new LinkedHashSet<>(group.getFollowerUserIds());
        for (String requestedId : requestedMemberIds) {
            if (requestedId == null || requestedId.isBlank()) {
                throw new BadRequestException("Every selected profile must have a valid ID.");
            }
            String canonicalId = eligibleMemberIds.get(normalize(requestedId));
            if (canonicalId == null) {
                throw new BadRequestException("You can add only your connections or your followers to this group.");
            }
            memberIds.removeIf(memberId -> normalize(memberId).equals(normalize(canonicalId)));
            if (!normalize(canonicalId).equals(normalize(ownerId))
                    && followerIds.stream().noneMatch(followerId -> normalize(followerId).equals(normalize(canonicalId)))) {
                followerIds.add(canonicalId);
            }
        }

        group.setMemberProfileIds(new ArrayList<>(memberIds));
        group.setFollowerUserIds(new ArrayList<>(followerIds));
        return toDto(groupRepository.save(group), ownerId);
    }

    @Transactional
    public Map<String, Object> followGroup(String userIdentifier, Long groupId) {
        String userId = resolveOwnerId(userIdentifier);
        ConnectionGroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BadRequestException("Could not find this group."));
        if (!group.getOwnerId().equals(userId)) {
            if (group.getFollowerUserIds().stream()
                    .noneMatch(followerId -> normalize(followerId).equals(normalize(userId)))) {
                group.getFollowerUserIds().add(userId);
            }
            group.getMemberProfileIds().removeIf(memberId -> normalize(memberId).equals(normalize(userId)));
            groupRepository.save(group);
        }
        return toDto(group, userId);
    }

    @Transactional
    public Map<String, Object> unfollowGroup(String userIdentifier, Long groupId) {
        String userId = resolveOwnerId(userIdentifier);
        ConnectionGroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BadRequestException("Could not find this group."));
        group.getFollowerUserIds().removeIf(followerId -> normalize(followerId).equals(normalize(userId)));
        groupRepository.save(group);
        return toDto(group, userId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGroupPosts(String userIdentifier, Long groupId) {
        String viewerId = resolveOwnerId(userIdentifier);
        ConnectionGroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BadRequestException("Could not find this group."));
        boolean canViewAll = isOrganizationPage(group)
                || group.getOwnerId().equals(viewerId)
                || group.getFollowerUserIds().stream().anyMatch(followerId -> normalize(followerId).equals(normalize(viewerId)));
        List<ConnectionGroupPostEntity> posts = groupPostRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId);
        Map<Long, List<ConnectionGroupPostActivityEntity>> activitiesByPostId = new LinkedHashMap<>();
        if (postActivityRepository != null && !posts.isEmpty()) {
            List<Long> postIds = posts.stream().map(ConnectionGroupPostEntity::getId).toList();
            postActivityRepository.findAllByPostIdInOrderByCreatedAtAsc(postIds)
                    .forEach(activity -> activitiesByPostId
                            .computeIfAbsent(activity.getPostId(), ignored -> new ArrayList<>())
                            .add(activity));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("canViewAll", canViewAll);
        if (canViewAll) {
            result.put("posts", posts.stream()
                    .map(post -> toPostDto(
                            post,
                            viewerId,
                            activitiesByPostId.getOrDefault(post.getId(), List.of())))
                    .toList());
            result.put("preview", null);
            return result;
        }

        result.put("posts", List.of());
        if (isBannerGroup(group)) {
            result.put("preview", null);
            return result;
        }
        result.put("preview", posts.stream()
                .filter(post -> post.getMediaUrl() != null
                        && post.getMediaType() != null
                        && post.getMediaType().toLowerCase(Locale.ROOT).startsWith("image/"))
                .findFirst()
                .map(post -> Map.<String, Object>of(
                        "imageUrl", post.getMediaUrl(),
                        "createdAt", post.getCreatedAt()))
                .orElse(null));
        return result;
    }

    @Transactional
    public Map<String, Object> recordGroupPostActivity(
            String userIdentifier,
            Long groupId,
            Long postId,
            String type,
            String content) {
        ResolvedOwner actor = resolveOwner(userIdentifier);
        ConnectionGroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BadRequestException("Could not find this page."));
        if (!isOrganizationPage(group)) {
            throw new BadRequestException("Engagement tracking is available only on organization pages.");
        }
        ConnectionGroupPostEntity post = groupPostRepository.findByIdAndGroupId(postId, groupId)
                .orElseThrow(() -> new BadRequestException("Could not find this page post."));
        if (postActivityRepository == null) {
            throw new IllegalStateException("Page post activity storage is not configured.");
        }

        String activityType = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("REACTION", "COMMENT", "IMPRESSION", "REPOST").contains(activityType)) {
            throw new BadRequestException("Choose a valid page post activity.");
        }
        boolean isOwner = normalize(group.getOwnerId()).equals(normalize(actor.id()));
        if (isOwner && !"IMPRESSION".equals(activityType)) {
            throw new BadRequestException("Page owners cannot engage with their own posts.");
        }

        List<ConnectionGroupPostActivityEntity> activities =
                postActivityRepository.findAllByPostIdOrderByCreatedAtAsc(postId);
        if ("COMMENT".equals(activityType)) {
            String cleanContent = content == null ? "" : content.trim();
            if (cleanContent.isEmpty() || cleanContent.length() > 1000) {
                throw new BadRequestException("Comments must be between 1 and 1,000 characters.");
            }
            savePostActivity(group, post, actor, activityType, cleanContent);
        } else if ("REACTION".equals(activityType)) {
            if (isOwner) return toPostDto(post, actor.id(), activities);
            List<ConnectionGroupPostActivityEntity> existingReactions = activities.stream()
                    .filter(activity -> "REACTION".equals(activity.getActivityType()))
                    .filter(activity -> normalize(activity.getActorId()).equals(normalize(actor.id())))
                    .toList();
            if (!existingReactions.isEmpty()) {
                existingReactions.forEach(postActivityRepository::delete);
            } else {
                savePostActivity(group, post, actor, activityType, null);
            }
        } else if (isOwner) {
            return toPostDto(post, actor.id(), activities);
        } else if ("IMPRESSION".equals(activityType)) {
            boolean alreadyCountedToday = activities.stream()
                    .filter(activity -> "IMPRESSION".equals(activity.getActivityType()))
                    .filter(activity -> normalize(activity.getActorId()).equals(normalize(actor.id())))
                    .anyMatch(activity -> activity.getCreatedAt().toLocalDate().equals(java.time.LocalDate.now()));
            if (!alreadyCountedToday) savePostActivity(group, post, actor, activityType, null);
        } else {
            boolean alreadyReposted = activities.stream()
                    .anyMatch(activity -> "REPOST".equals(activity.getActivityType())
                            && normalize(activity.getActorId()).equals(normalize(actor.id())));
            if (!alreadyReposted) savePostActivity(group, post, actor, activityType, null);
        }

        return toPostDto(
                post,
                actor.id(),
                postActivityRepository.findAllByPostIdOrderByCreatedAtAsc(postId));
    }

    private void savePostActivity(
            ConnectionGroupEntity group,
            ConnectionGroupPostEntity post,
            ResolvedOwner actor,
            String type,
            String content) {
        ConnectionGroupPostActivityEntity activity = new ConnectionGroupPostActivityEntity();
        activity.setGroupId(group.getId());
        activity.setPostId(post.getId());
        activity.setActorId(actor.id());
        activity.setActorName(actor.name() == null || actor.name().isBlank() ? "Member" : actor.name().trim());
        activity.setActivityType(type);
        activity.setContent(content);
        postActivityRepository.save(activity);
    }

    @Transactional
    public Map<String, Object> createGroupPost(
            String ownerIdentifier,
            Long groupId,
            String content,
            String mediaUrl,
            String mediaType) {
        ResolvedOwner owner = resolveOwner(ownerIdentifier);
        String ownerId = owner.id();
        ConnectionGroupEntity group = groupRepository.findByIdAndOwnerId(groupId, ownerId)
                .orElseThrow(() -> new BadRequestException("Only the group creator can publish group updates."));

        String cleanContent = content == null ? "" : content.trim();
        String cleanMediaUrl = mediaUrl == null ? "" : mediaUrl.trim();
        String cleanMediaType = mediaType == null ? "" : mediaType.trim().toLowerCase(Locale.ROOT);
        if (isBannerGroup(group) && (!cleanContent.isEmpty() || cleanMediaUrl.isEmpty())) {
            throw new BadRequestException("Banner updates must contain an image or video only.");
        }
        if (cleanContent.isEmpty() && cleanMediaUrl.isEmpty()) {
            throw new BadRequestException("Add text or attach an image or video before posting.");
        }
        if (cleanContent.length() > 10000) {
            throw new BadRequestException("Group post text must be 10,000 characters or fewer.");
        }
        if (cleanMediaUrl.length() > 2048) {
            throw new BadRequestException("Group post media URLs must be under 2048 characters.");
        }
        if (!cleanMediaUrl.isEmpty()
                && !(cleanMediaType.startsWith("image/") || cleanMediaType.startsWith("video/"))) {
            throw new BadRequestException("Group attachments must be an image or video.");
        }

        ConnectionGroupPostEntity post = new ConnectionGroupPostEntity();
        post.setGroupId(group.getId());
        post.setAuthorId(ownerId);
        post.setAuthorName(owner.name() == null || owner.name().isBlank()
                ? "Group creator"
                : owner.name().trim());
        post.setContent(cleanContent);
        post.setMediaUrl(cleanMediaUrl.isEmpty() ? null : cleanMediaUrl);
        post.setMediaType(cleanMediaUrl.isEmpty() ? null : cleanMediaType);
        return toPostDto(groupPostRepository.save(post), ownerId, List.of());
    }

    @Transactional
    public void deleteGroupPost(String userIdentifier, Long groupId, Long postId) {
        String ownerId = resolveOwnerId(userIdentifier);
        ConnectionGroupEntity group = groupRepository.findByIdAndOwnerId(groupId, ownerId)
                .orElseThrow(() -> new BadRequestException("Only the group creator can delete group updates."));
        ConnectionGroupPostEntity post = groupPostRepository.findByIdAndGroupId(postId, group.getId())
                .orElseThrow(() -> new BadRequestException("Could not find this group update."));
        if (!ownerId.equals(post.getAuthorId())) {
            throw new BadRequestException("Only the person who posted this update can delete it.");
        }
        if (postActivityRepository != null) postActivityRepository.deleteAllByPostId(postId);
        groupPostRepository.delete(post);
    }

    @Transactional
    public void deleteGroup(String ownerIdentifier, Long groupId) {
        String ownerId = resolveOwnerId(ownerIdentifier);
        ConnectionGroupEntity group = groupRepository.findByIdAndOwnerId(groupId, ownerId)
                .orElseThrow(() -> new BadRequestException("Could not find this group."));
        if (postActivityRepository != null) postActivityRepository.deleteAllByGroupId(group.getId());
        groupPostRepository.deleteAllByGroupId(group.getId());
        groupRepository.delete(group);
    }

    private Map<String, Object> toPostDto(
            ConnectionGroupPostEntity post,
            String viewerId,
            List<ConnectionGroupPostActivityEntity> activities) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", post.getId());
        result.put("authorId", post.getAuthorId());
        result.put("authorName", post.getAuthorName());
        result.put("content", post.getContent());
        result.put("mediaUrl", post.getMediaUrl());
        result.put("mediaType", post.getMediaType());
        result.put("createdAt", post.getCreatedAt());
        List<Map<String, Object>> reactions = new ArrayList<>();
        List<Map<String, Object>> comments = new ArrayList<>();
        List<Map<String, Object>> impressions = new ArrayList<>();
        List<Map<String, Object>> reposts = new ArrayList<>();
        boolean viewerReacted = false;
        boolean viewerReposted = false;
        for (ConnectionGroupPostActivityEntity activity : activities) {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("id", activity.getId());
            event.put("createdAt", activity.getCreatedAt());
            switch (activity.getActivityType()) {
                case "REACTION" -> {
                    reactions.add(event);
                    if (viewerId != null && normalize(activity.getActorId()).equals(normalize(viewerId))) {
                        viewerReacted = true;
                    }
                }
                case "COMMENT" -> {
                    event.put("authorName", activity.getActorName());
                    event.put("content", activity.getContent());
                    comments.add(event);
                }
                case "IMPRESSION" -> impressions.add(event);
                case "REPOST" -> {
                    reposts.add(event);
                    if (viewerId != null && normalize(activity.getActorId()).equals(normalize(viewerId))) {
                        viewerReposted = true;
                    }
                }
                default -> { }
            }
        }
        result.put("reactions", reactions);
        result.put("comments", comments);
        result.put("impressions", impressions);
        result.put("reposts", reposts);
        result.put("viewerReacted", viewerReacted);
        result.put("viewerReposted", viewerReposted);
        return result;
    }

    private String normalizeImageUrl(String imageUrl) {
        String cleanImageUrl = imageUrl == null ? "" : imageUrl.trim();
        return cleanImageUrl.isEmpty() ? null : cleanImageUrl;
    }

    private void applyOrganizationPageFields(
            ConnectionGroupEntity page,
            String name,
            String publicUrl,
            String website,
            String industry,
            String tagline,
            String logo,
            String bannerUrl) {
        String cleanName = name == null ? "" : name.trim();
        String cleanPublicUrl = publicUrl == null ? "" : publicUrl.trim();
        String cleanIndustry = industry == null ? "" : industry.trim();
        if (cleanName.isEmpty() || cleanName.length() > MAX_ORGANIZATION_PAGE_NAME_LENGTH) {
            throw new BadRequestException("A page name of 1 to 100 characters is required.");
        }
        if (!cleanPublicUrl.matches("[A-Za-z0-9][A-Za-z0-9-]{0,61}[A-Za-z0-9]")) {
            throw new BadRequestException("Use 2 to 63 letters, numbers, or hyphens for the public page URL.");
        }
        if (cleanIndustry.isEmpty() || cleanIndustry.length() > 100) {
            throw new BadRequestException("An industry of 1 to 100 characters is required.");
        }
        page.setName(cleanName);
        page.setPublicUrl(cleanPublicUrl);
        page.setWebsite(normalizeOptionalText(website, 255, "Website"));
        page.setIndustry(cleanIndustry);
        page.setTagline(normalizeOptionalText(tagline, 120, "Tagline"));
        page.setImageUrl(normalizeImageUrl(logo));
        page.setBannerUrl(normalizeImageUrl(bannerUrl));
    }

    private String normalizeOptionalText(String value, int maxLength, String fieldName) {
        String cleanValue = value == null ? "" : value.trim();
        if (cleanValue.length() > maxLength) {
            throw new BadRequestException(fieldName + " must be " + maxLength + " characters or fewer.");
        }
        return cleanValue.isEmpty() ? null : cleanValue;
    }

    private void validateGroupName(String groupName) {
        if (groupName.isEmpty() || groupName.length() > MAX_GROUP_NAME_LENGTH) {
            throw new BadRequestException("A group name of 1 to 80 characters is required.");
        }
    }

    private Set<String> resolveConnectedMemberIds(
            String ownerIdentifier,
            List<String> requestedMemberIds,
            boolean allowEmpty) {
        if (requestedMemberIds == null || (!allowEmpty && requestedMemberIds.isEmpty())) {
            throw new BadRequestException("Select at least one connection for this group.");
        }

        Map<String, String> connectedMemberIds = new LinkedHashMap<>();
        for (Map<String, Object> profile : connectionService.getConnectedProfiles(ownerIdentifier)) {
            String canonicalId = firstValue(profile, "userId", "publicUserId", "id", "profileId");
            if (canonicalId == null) continue;
            for (String key : identityValues(profile)) {
                connectedMemberIds.putIfAbsent(normalize(key), canonicalId);
            }
        }

        Set<String> selectedMemberIds = new LinkedHashSet<>();
        for (String requestedId : requestedMemberIds) {
            if (requestedId == null || requestedId.isBlank()) {
                throw new BadRequestException("Every selected connection must have a valid profile ID.");
            }
            String canonicalId = connectedMemberIds.get(normalize(requestedId));
            if (canonicalId == null) {
                throw new BadRequestException("Groups can include only your current connections.");
            }
            selectedMemberIds.add(canonicalId);
        }
        return selectedMemberIds;
    }

    private String resolveOwnerId(String ownerIdentifier) {
        return resolveOwner(ownerIdentifier).id();
    }

    private ResolvedOwner resolveOwner(String ownerIdentifier) {
        if (ownerIdentifier == null || ownerIdentifier.isBlank()) {
            throw new BadRequestException("A signed-in account is required to manage connection groups.");
        }
        String identifier = ownerIdentifier.trim();
        Optional<UserAccountEntity> account = userAccountRepository.findByEmail(identifier)
                .or(() -> userAccountRepository.findByPublicUserId(identifier));
        if (account.isEmpty()) {
            try {
                account = userAccountRepository.findById(Long.parseLong(identifier));
            } catch (NumberFormatException ignored) {
                account = Optional.empty();
            }
        }
        if (account.isPresent()) {
            UserAccountEntity resolvedAccount = account.get();
            String ownerId = resolvedAccount.getPublicUserId() == null || resolvedAccount.getPublicUserId().isBlank()
                    ? String.valueOf(resolvedAccount.getId())
                    : resolvedAccount.getPublicUserId();
            return new ResolvedOwner(ownerId, resolvedAccount.getName());
        }

        if (profileRepository != null) {
            Optional<ProfileEntity> profile = profileRepository.findByEmail(identifier)
                    .or(() -> profileRepository.findByUserId(identifier));
            if (profile.isEmpty()) {
                try {
                    profile = profileRepository.findById(Long.parseLong(identifier));
                } catch (NumberFormatException ignored) {
                    profile = Optional.empty();
                }
            }
            if (profile.isPresent()) {
                ProfileEntity resolvedProfile = profile.get();
                String ownerId = resolvedProfile.getUserId() == null || resolvedProfile.getUserId().isBlank()
                        ? identifier
                        : resolvedProfile.getUserId();
                return new ResolvedOwner(ownerId, resolvedProfile.getName());
            }
        }

        String fallbackName = "Cinema Professional";
        if (identifier.contains("@")) {
            String prefix = identifier.substring(0, identifier.indexOf('@')).trim();
            if (!prefix.isEmpty()) {
                fallbackName = prefix;
            }
        }
        return new ResolvedOwner(identifier, fallbackName);
    }

    private record ResolvedOwner(String id, String name) {}

    private Set<String> resolveViewerIdentities(String ownerIdentifier) {
        Set<String> identities = new LinkedHashSet<>();
        if (ownerIdentifier == null || ownerIdentifier.isBlank()) {
            return identities;
        }
        String clean = ownerIdentifier.trim();
        identities.add(clean);
        identities.add(clean.toLowerCase(Locale.ROOT));

        try {
            Optional<UserAccountEntity> account = userAccountRepository.findByEmail(clean)
                    .or(() -> userAccountRepository.findByPublicUserId(clean));
            if (account.isEmpty()) {
                try {
                    account = userAccountRepository.findById(Long.parseLong(clean));
                } catch (NumberFormatException ignored) {}
            }
            account.ifPresent(acc -> {
                if (acc.getId() != null) {
                    identities.add(String.valueOf(acc.getId()));
                }
                if (acc.getPublicUserId() != null && !acc.getPublicUserId().isBlank()) {
                    identities.add(acc.getPublicUserId().trim());
                    identities.add(acc.getPublicUserId().trim().toLowerCase(Locale.ROOT));
                }
                if (acc.getEmail() != null && !acc.getEmail().isBlank()) {
                    identities.add(acc.getEmail().trim());
                    identities.add(acc.getEmail().trim().toLowerCase(Locale.ROOT));
                }
            });
        } catch (Exception ignored) {}

        if (profileRepository != null) {
            try {
                Optional<ProfileEntity> profile = profileRepository.findByEmail(clean)
                        .or(() -> profileRepository.findByUserId(clean));
                if (profile.isEmpty()) {
                    try {
                        profile = profileRepository.findById(Long.parseLong(clean));
                    } catch (NumberFormatException ignored) {}
                }
                profile.ifPresent(prof -> {
                    if (prof.getUserId() != null && !prof.getUserId().isBlank()) {
                        identities.add(prof.getUserId().trim());
                        identities.add(prof.getUserId().trim().toLowerCase(Locale.ROOT));
                    }
                    if (prof.getEmail() != null && !prof.getEmail().isBlank()) {
                        identities.add(prof.getEmail().trim());
                        identities.add(prof.getEmail().trim().toLowerCase(Locale.ROOT));
                    }
                });
            } catch (Exception ignored) {}
        }
        return identities;
    }

    private Map<String, Object> toDto(ConnectionGroupEntity group, String viewerId) {
        return toDto(group, resolveViewerIdentities(viewerId));
    }

    private Map<String, Object> toDto(ConnectionGroupEntity group, Set<String> viewerIdentities) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", group.getId());
        result.put("name", group.getName());
        result.put("groupType", group.getGroupType());
        result.put("isBanner", isBannerGroup(group));
        result.put("isOrganizationPage", isOrganizationPage(group));
        result.put("ownerName", group.getOwnerName());
        result.put("publicUrl", group.getPublicUrl());
        result.put("website", group.getWebsite());
        result.put("industry", group.getIndustry());
        result.put("tagline", group.getTagline());
        result.put("imageUrl", group.getImageUrl());
        result.put("bannerUrl", group.getBannerUrl());
        result.put("memberProfileIds", group.getMemberProfileIds() != null ? new ArrayList<>(group.getMemberProfileIds()) : new ArrayList<>());
        boolean isOwner = false;
        if (group.getOwnerId() != null && !group.getOwnerId().isBlank() && viewerIdentities != null && !viewerIdentities.isEmpty()) {
            isOwner = viewerIdentities.stream().anyMatch(id -> id.equalsIgnoreCase(group.getOwnerId()));
        }
        boolean isFollowing = false;
        if (group.getFollowerUserIds() != null && viewerIdentities != null && !viewerIdentities.isEmpty()) {
            isFollowing = group.getFollowerUserIds().stream().anyMatch(f ->
                    f != null && viewerIdentities.stream().anyMatch(v -> v.equalsIgnoreCase(f)));
        }
        result.put("isOwner", isOwner);
        result.put("ownerId", isOrganizationPage(group) && !isOwner ? null : group.getOwnerId());
        result.put("isFollowing", isFollowing);
        result.put("followerCount", group.getFollowerUserIds() != null ? group.getFollowerUserIds().size() : 0);
        boolean isSubscribed = group.getSubscriberUserIds() != null && viewerIdentities != null
                && group.getSubscriberUserIds().stream().anyMatch(subscriberId ->
                        viewerIdentities.stream().anyMatch(viewerId -> viewerId.equalsIgnoreCase(subscriberId)));
        result.put("isSubscribed", isSubscribed);
        result.put("subscriberCount", group.getSubscriberUserIds() != null ? group.getSubscriberUserIds().size() : 0);
        boolean isSubscriptionPending = false;
        if (isOrganizationPage(group)
                && !isOwner
                && group.getId() != null
                && subscriptionRequestRepository != null
                && viewerIdentities != null
                && !viewerIdentities.isEmpty()) {
            isSubscriptionPending = subscriptionRequestRepository
                    .existsByPageIdAndRequesterIdInAndStatusIgnoreCase(
                            group.getId(), viewerIdentities, "PENDING");
        }
        result.put("isSubscriptionPending", isSubscriptionPending);
        result.put("subscriberUserIds", group.getSubscriberUserIds() != null && isOwner
                ? new ArrayList<>(group.getSubscriberUserIds())
                : new ArrayList<>());
        List<String> subscriberNames = new ArrayList<>();
        if (isOwner
                && isOrganizationPage(group)
                && group.getId() != null
                && subscriptionRequestRepository != null
                && group.getSubscriberUserIds() != null) {
            Set<String> activeSubscriberIds = group.getSubscriberUserIds().stream()
                    .map(this::normalize)
                    .collect(java.util.stream.Collectors.toSet());
            subscriberNames = subscriptionRequestRepository
                    .findAllByPageIdAndStatusIgnoreCaseOrderByCreatedAtDesc(group.getId(), "ACCEPTED")
                    .stream()
                    .filter(request -> activeSubscriberIds.contains(normalize(request.getRequesterId())))
                    .map(OrganizationPageSubscriptionRequestEntity::getRequesterName)
                    .distinct()
                    .toList();
        }
        result.put("subscriberNames", subscriberNames);
        result.put("followerUserIds", group.getFollowerUserIds() != null
                && (isOwner || (!isBannerGroup(group) && !isOrganizationPage(group)))
                        ? new ArrayList<>(group.getFollowerUserIds())
                        : new ArrayList<>());
        result.put("createdAt", group.getCreatedAt());
        return result;
    }

    private boolean isBannerGroup(ConnectionGroupEntity group) {
        return "BANNER".equalsIgnoreCase(group.getGroupType());
    }

    private boolean isOrganizationPage(ConnectionGroupEntity group) {
        return "ORG_PAGE".equalsIgnoreCase(group.getGroupType());
    }

    private List<String> identityValues(Map<String, Object> profile) {
        List<String> values = new ArrayList<>();
        for (String key : List.of("userId", "publicUserId", "id", "profileId", "email")) {
            Object value = profile.get(key);
            if (value != null && !value.toString().isBlank()) {
                values.add(value.toString());
            }
        }
        return values;
    }

    private String firstValue(Map<String, Object> profile, String... keys) {
        for (String key : keys) {
            Object value = profile.get(key);
            if (value != null && !value.toString().isBlank()) return value.toString().trim();
        }
        return null;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String displayName(String name, String fallback) {
        return name == null || name.isBlank() ? fallback : name.trim();
    }
}
