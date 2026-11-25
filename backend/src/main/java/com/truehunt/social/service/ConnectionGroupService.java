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
        String viewerId = resolveOwnerId(ownerIdentifier);
        return groupRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(group -> toDto(group, viewerId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConnectionGroupEntity getGroupForMessaging(String ownerIdentifier, Long groupId) {
        String ownerId = resolveOwnerId(ownerIdentifier);
        return groupRepository.findByIdAndOwnerId(groupId, ownerId)
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
        group.setMemberProfileIds(new ArrayList<>(selectedMemberIds));
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
        selectedMemberIds.addAll(group.getFollowerUserIds());
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
        for (String requestedId : requestedMemberIds) {
            if (requestedId == null || requestedId.isBlank()) {
                throw new BadRequestException("Every selected profile must have a valid ID.");
            }
            String canonicalId = eligibleMemberIds.get(normalize(requestedId));
            if (canonicalId == null) {
                throw new BadRequestException("You can add only your connections or your followers to this group.");
            }
            if (memberIds.stream().noneMatch(memberId -> normalize(memberId).equals(normalize(canonicalId)))) {
                memberIds.add(canonicalId);
            }
        }

        group.setMemberProfileIds(new ArrayList<>(memberIds));
        return toDto(groupRepository.save(group), ownerId);
    }

    @Transactional
    public Map<String, Object> followGroup(String userIdentifier, Long groupId) {
        String userId = resolveOwnerId(userIdentifier);
        ConnectionGroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BadRequestException("Could not find this group."));
        if (!group.getOwnerId().equals(userId)) {
            if (!group.getFollowerUserIds().contains(userId)) {
                group.getFollowerUserIds().add(userId);
            }
            if (!group.getMemberProfileIds().contains(userId)) {
                group.getMemberProfileIds().add(userId);
            }
            groupRepository.save(group);
        }
        return toDto(group, userId);
    }

    @Transactional
    public Map<String, Object> unfollowGroup(String userIdentifier, Long groupId) {
        String userId = resolveOwnerId(userIdentifier);
        ConnectionGroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BadRequestException("Could not find this group."));
        group.getFollowerUserIds().removeIf(userId::equals);
        group.getMemberProfileIds().removeIf(userId::equals);
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
        if (cleanImageUrl.length() > 2048) {
            throw new BadRequestException("Group photos must use an image URL under 2048 characters.");
        }
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

        throw new BadRequestException("Could not resolve the account for these connection groups.");
    }

    private record ResolvedOwner(String id, String name) {}

    private Map<String, Object> toDto(ConnectionGroupEntity group, String viewerId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", group.getId());
        result.put("name", group.getName());
        result.put("imageUrl", group.getImageUrl());
        result.put("memberProfileIds", new ArrayList<>(group.getMemberProfileIds()));
        result.put("ownerId", group.getOwnerId());
        result.put("isOwner", group.getOwnerId().equals(viewerId));
        result.put("isFollowing", group.getFollowerUserIds().contains(viewerId));
        result.put("followerCount", group.getFollowerUserIds().size());
        result.put("followerUserIds", new ArrayList<>(group.getFollowerUserIds()));
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
