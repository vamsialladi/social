package com.truehunt.social.service;

import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.model.ConnectionGroupEntity;
import com.truehunt.social.model.ConnectionGroupPostEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ConnectionGroupRepository;
import com.truehunt.social.repository.ConnectionGroupPostRepository;
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

    private final ConnectionGroupRepository groupRepository;
    private final ConnectionGroupPostRepository groupPostRepository;
    private final UserAccountRepository userAccountRepository;
    private final ConnectionService connectionService;
    private final ConversationRepository conversationRepository;
    private final ProfileRepository profileRepository;

    public ConnectionGroupService(
            ConnectionGroupRepository groupRepository,
            ConnectionGroupPostRepository groupPostRepository,
            UserAccountRepository userAccountRepository,
            ConnectionService connectionService) {
        this(groupRepository, groupPostRepository, userAccountRepository, connectionService, null, null);
    }

    public ConnectionGroupService(
            ConnectionGroupRepository groupRepository,
            ConnectionGroupPostRepository groupPostRepository,
            UserAccountRepository userAccountRepository,
            ConnectionService connectionService,
            ConversationRepository conversationRepository) {
        this(groupRepository, groupPostRepository, userAccountRepository, connectionService,
                conversationRepository, null);
    }

    @Autowired
    public ConnectionGroupService(
            ConnectionGroupRepository groupRepository,
            ConnectionGroupPostRepository groupPostRepository,
            UserAccountRepository userAccountRepository,
            ConnectionService connectionService,
            ConversationRepository conversationRepository,
            ProfileRepository profileRepository) {
        this.groupRepository = groupRepository;
        this.groupPostRepository = groupPostRepository;
        this.userAccountRepository = userAccountRepository;
        this.connectionService = connectionService;
        this.conversationRepository = conversationRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGroups(String ownerIdentifier) {
        Set<String> viewerIdentities = resolveViewerIdentities(ownerIdentifier);
        return groupRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(group -> toDto(group, viewerIdentities))
                .toList();
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
        boolean canViewAll = group.getOwnerId().equals(viewerId) || group.getFollowerUserIds().contains(viewerId);
        List<ConnectionGroupPostEntity> posts = groupPostRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("canViewAll", canViewAll);
        if (canViewAll) {
            result.put("posts", posts.stream().map(this::toPostDto).toList());
            result.put("preview", null);
            return result;
        }

        result.put("posts", List.of());
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
        return toPostDto(groupPostRepository.save(post));
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
        groupPostRepository.delete(post);
    }

    @Transactional
    public void deleteGroup(String ownerIdentifier, Long groupId) {
        String ownerId = resolveOwnerId(ownerIdentifier);
        ConnectionGroupEntity group = groupRepository.findByIdAndOwnerId(groupId, ownerId)
                .orElseThrow(() -> new BadRequestException("Could not find this group."));
        groupPostRepository.deleteAllByGroupId(group.getId());
        groupRepository.delete(group);
    }

    private Map<String, Object> toPostDto(ConnectionGroupPostEntity post) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", post.getId());
        result.put("authorId", post.getAuthorId());
        result.put("authorName", post.getAuthorName());
        result.put("content", post.getContent());
        result.put("mediaUrl", post.getMediaUrl());
        result.put("mediaType", post.getMediaType());
        result.put("createdAt", post.getCreatedAt());
        return result;
    }

    private String normalizeImageUrl(String imageUrl) {
        String cleanImageUrl = imageUrl == null ? "" : imageUrl.trim();
        return cleanImageUrl.isEmpty() ? null : cleanImageUrl;
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
        result.put("imageUrl", group.getImageUrl());
        result.put("memberProfileIds", group.getMemberProfileIds() != null ? new ArrayList<>(group.getMemberProfileIds()) : new ArrayList<>());
        result.put("ownerId", group.getOwnerId());
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
        result.put("isFollowing", isFollowing);
        result.put("followerCount", group.getFollowerUserIds() != null ? group.getFollowerUserIds().size() : 0);
        result.put("followerUserIds", group.getFollowerUserIds() != null ? new ArrayList<>(group.getFollowerUserIds()) : new ArrayList<>());
        result.put("createdAt", group.getCreatedAt());
        return result;
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
}
