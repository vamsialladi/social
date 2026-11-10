package com.truehunt.social.service;

import com.truehunt.social.dto.ConnectionRequestDto;
import com.truehunt.social.dto.TalentDto;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.model.NetworkProfileEntity;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.model.UserProfileRelationshipEntity;
import com.truehunt.social.repository.NetworkProfileRepository;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import com.truehunt.social.repository.UserProfileRelationshipRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConnectionService {

    private final NetworkProfileRepository networkProfileRepository;
    private final NotificationRepository notificationRepository;
    private final ProfileRepository profileRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRelationshipRepository relationshipRepository;
    private final ObjectProvider<TalentService> talentServiceProvider;

    public ConnectionService(NetworkProfileRepository networkProfileRepository,
            NotificationRepository notificationRepository,
            ProfileRepository profileRepository,
            UserAccountRepository userAccountRepository,
            UserProfileRelationshipRepository relationshipRepository,
            ObjectProvider<TalentService> talentServiceProvider) {
        this.networkProfileRepository = networkProfileRepository;
        this.notificationRepository = notificationRepository;
        this.profileRepository = profileRepository;
        this.userAccountRepository = userAccountRepository;
        this.relationshipRepository = relationshipRepository;
        this.talentServiceProvider = talentServiceProvider;
    }

    public List<Map<String, Object>> getSuggestions(String userEmail) {
        Set<String> ownerIds = getOwnerUserIdentifiers(userEmail);
        Set<String> lowerOwnerIds = ownerIds.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        Map<String, UserProfileRelationshipEntity> relationships = new HashMap<>();
        for (String id : ownerIds) {
            relationshipRepository.findAllByOwnerUserId(id)
                    .forEach(r -> relationships.put(r.getTargetProfileId(), r));
        }

        Map<String, Map<String, Object>> profiles = new LinkedHashMap<>();
        for (NetworkProfileEntity profile : networkProfileRepository.findAll()) {
            if (profile.getId() != null && lowerOwnerIds.contains(profile.getId().toLowerCase(Locale.ROOT))) {
                continue;
            }
            Map<String, Object> dto = profile.toDto();
            String profileId = profile.getId();
            UserProfileRelationshipEntity relationship = relationships.get(profileId);
            boolean isConnected = relationship != null && relationship.isConnected();
            boolean isFollowing = relationship != null && relationship.isFollowing();
            boolean isPending = relationship != null && "PENDING".equalsIgnoreCase(relationship.getStatus());
            String sender = relationship != null && relationship.getSenderUserId() != null
                    ? relationship.getSenderUserId().toLowerCase(Locale.ROOT)
                    : "";
            boolean isOutgoingPending = isPending && lowerOwnerIds.contains(sender);
            boolean isIncomingPending = isPending && !isOutgoingPending;

            dto.put("isConnected", isConnected);
            dto.put("connected", isConnected);
            dto.put("isFollowing", isFollowing);
            dto.put("following", isFollowing);
            dto.put("isPending", isPending);
            dto.put("isOutgoingPending", isOutgoingPending);
            dto.put("isIncomingPending", isIncomingPending);
            dto.put("connectionStatus", isConnected ? "CONNECTED"
                    : (isPending ? (isOutgoingPending ? "PENDING_OUTGOING" : "PENDING_INCOMING") : "NONE"));
            profiles.put(profileId, dto);
        }

        for (UserProfileRelationshipEntity relationship : relationships.values()) {
            String targetId = relationship.getTargetProfileId();
            if (targetId != null && !lowerOwnerIds.contains(targetId.toLowerCase(Locale.ROOT))
                    && !isInvalidTarget(targetId)) {
                profiles.computeIfAbsent(
                        targetId,
                        profileId -> {
                            Map<String, Object> dto = getProfileDto(profileId, relationship.isConnected(), relationship.isFollowing());
                            boolean isPend = "PENDING".equalsIgnoreCase(relationship.getStatus());
                            String sender = relationship.getSenderUserId() != null
                                    ? relationship.getSenderUserId().toLowerCase(Locale.ROOT)
                                    : "";
                            boolean isOutPending = isPend && lowerOwnerIds.contains(sender);
                            boolean isInPending = isPend && !isOutPending;
                            dto.put("isPending", isPend);
                            dto.put("isOutgoingPending", isOutPending);
                            dto.put("isIncomingPending", isInPending);
                            dto.put("connectionStatus", relationship.isConnected() ? "CONNECTED"
                                    : (isPend ? (isOutPending ? "PENDING_OUTGOING" : "PENDING_INCOMING") : "NONE"));
                            return dto;
                        });
            }
        }
        return new ArrayList<>(profiles.values());
    }

    public List<Map<String, Object>> getInvitations(String userEmail) {
        Set<String> ownerIds = getOwnerUserIdentifiers(userEmail);
        Set<String> lowerOwnerIds = ownerIds.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        List<UserProfileRelationshipEntity> allRels = relationshipRepository.findAll();
        Map<String, Map<String, Object>> invitationMap = new LinkedHashMap<>();

        for (UserProfileRelationshipEntity rel : allRels) {
            if (rel == null || !"PENDING".equalsIgnoreCase(rel.getStatus())) {
                continue;
            }
            String owner = rel.getOwnerUserId() != null ? rel.getOwnerUserId().trim().toLowerCase(Locale.ROOT) : "";
            String target = rel.getTargetProfileId() != null ? rel.getTargetProfileId().trim().toLowerCase(Locale.ROOT) : "";
            String sender = rel.getSenderUserId() != null ? rel.getSenderUserId().trim().toLowerCase(Locale.ROOT) : "";

            // Check if current user is the recipient of this invitation
            boolean isRecipient = (lowerOwnerIds.contains(owner) && !lowerOwnerIds.contains(sender))
                    || (lowerOwnerIds.contains(target) && !lowerOwnerIds.contains(sender));

            if (!isRecipient) {
                continue;
            }

            // The sender profile ID is the one that is NOT the current user
            String senderProfileId = !lowerOwnerIds.contains(sender) && !sender.isBlank()
                    ? rel.getSenderUserId().trim()
                    : (lowerOwnerIds.contains(owner) ? rel.getTargetProfileId().trim() : rel.getOwnerUserId().trim());

            if (isInvalidTarget(senderProfileId) || lowerOwnerIds.contains(senderProfileId.toLowerCase(Locale.ROOT))) {
                continue;
            }

            if (!invitationMap.containsKey(senderProfileId)) {
                Map<String, Object> senderDto = getProfileDto(senderProfileId, false, false);
                Map<String, Object> inv = new LinkedHashMap<>();
                inv.put("id", rel.getId());
                inv.put("invitationId", rel.getId());
                inv.put("profileId", senderProfileId);
                inv.put("senderId", senderProfileId);
                inv.put("name", senderDto.get("name"));
                inv.put("role", senderDto.get("role"));
                inv.put("location", senderDto.get("location"));
                inv.put("avatar", senderDto.get("avatar"));
                inv.put("profileImageUrl", senderDto.get("profileImageUrl"));
                inv.put("initials", senderDto.get("initials"));
                inv.put("mutualConnections", "Cinema Industry member");
                inv.put("createdAt", rel.getCreatedAt() != null ? rel.getCreatedAt().toString() : "");
                inv.put("profile", senderDto);
                invitationMap.put(senderProfileId, inv);
            }
        }

        return new ArrayList<>(invitationMap.values());
    }

    public List<Map<String, Object>> getConnectedProfiles(String userEmail) {
        Set<String> ownerIds = getOwnerUserIdentifiers(userEmail);
        Set<String> lowerOwnerIds = ownerIds.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        List<UserProfileRelationshipEntity> allRels = relationshipRepository.findAll();
        Map<String, Map<String, Object>> connectedMap = new LinkedHashMap<>();

        for (UserProfileRelationshipEntity rel : allRels) {
            if (!rel.isConnected())
                continue;
            String owner = rel.getOwnerUserId() != null ? rel.getOwnerUserId().trim() : "";
            String target = rel.getTargetProfileId() != null ? rel.getTargetProfileId().trim() : "";

            if (lowerOwnerIds.contains(owner.toLowerCase(Locale.ROOT))) {
                if (!lowerOwnerIds.contains(target.toLowerCase(Locale.ROOT)) && !isInvalidTarget(target)) {
                    connectedMap.putIfAbsent(target, getProfileDto(target, true, rel.isFollowing()));
                }
            } else if (lowerOwnerIds.contains(target.toLowerCase(Locale.ROOT))) {
                if (!lowerOwnerIds.contains(owner.toLowerCase(Locale.ROOT)) && !isInvalidTarget(owner)) {
                    connectedMap.putIfAbsent(owner, getProfileDto(owner, true, rel.isFollowing()));
                }
            }
        }
        return new ArrayList<>(connectedMap.values());
    }

    @Transactional
    public Map<String, Object> sendConnectionRequest(String profileId, String message, String userEmail) {
        String targetProfileId = requireProfileId(profileId);
        Set<String> ownerIds = getOwnerUserIdentifiers(userEmail);
        Set<String> lowerOwnerIds = ownerIds.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        if (lowerOwnerIds.contains(targetProfileId.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("You cannot connect with your own profile");
        }

        String primaryOwnerId = getPrimaryOwnerUserId(userEmail);
        UserProfileRelationshipEntity relationship = getOrCreateRelationship(primaryOwnerId, targetProfileId);
        UserProfileRelationshipEntity reciprocal = getOrCreateRelationship(targetProfileId, primaryOwnerId);

        if (relationship.isConnected()) {
            Map<String, Object> res = new HashMap<>();
            res.put("id", targetProfileId);
            res.put("profileId", targetProfileId);
            res.put("connected", true);
            res.put("isConnected", true);
            res.put("isPending", false);
            res.put("status", "ACCEPTED");
            res.put("message", "Already connected with " + getProfileName(targetProfileId));
            return res;
        }

        // If target already sent us an invitation, sending connect is accepting it!
        if ("PENDING".equalsIgnoreCase(relationship.getStatus())
                && relationship.getSenderUserId() != null
                && !lowerOwnerIds.contains(relationship.getSenderUserId().toLowerCase(Locale.ROOT))) {
            return acceptConnection(targetProfileId, userEmail);
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        relationship.setConnected(false);
        relationship.setStatus("PENDING");
        relationship.setSenderUserId(primaryOwnerId);
        relationship.setCreatedAt(now);
        relationship.setUpdatedAt(now);
        relationshipRepository.save(relationship);

        reciprocal.setConnected(false);
        reciprocal.setStatus("PENDING");
        reciprocal.setSenderUserId(primaryOwnerId);
        reciprocal.setCreatedAt(now);
        reciprocal.setUpdatedAt(now);
        relationshipRepository.save(reciprocal);

        String targetName = getProfileName(targetProfileId);
        String ownerName = getProfileName(primaryOwnerId);

        notificationRepository.save(new NotificationEntity(
                targetProfileId,
                "New Connection Request",
                ownerName + " sent you an invitation to connect on TrueHunt.",
                "connection_request"));

        notificationRepository.save(new NotificationEntity(
                primaryOwnerId,
                "Invitation Sent",
                "You sent a connection invitation to " + targetName + ".",
                "connection_request"));

        Map<String, Object> res = new HashMap<>();
        res.put("id", targetProfileId);
        res.put("profileId", targetProfileId);
        res.put("connected", false);
        res.put("isConnected", false);
        res.put("isPending", true);
        res.put("isOutgoingPending", true);
        res.put("status", "PENDING");
        res.put("message", "Connection request sent to " + targetName);
        return res;
    }

    @Transactional
    public Map<String, Object> acceptConnection(String profileId, String userEmail) {
        String requesterProfileId = requireProfileId(profileId);
        Set<String> ownerIds = getOwnerUserIdentifiers(userEmail);
        Set<String> lowerOwnerIds = ownerIds.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        if (lowerOwnerIds.contains(requesterProfileId.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("You cannot connect with your own profile");
        }

        String primaryOwnerId = getPrimaryOwnerUserId(userEmail);
        UserProfileRelationshipEntity relationship = getOrCreateRelationship(primaryOwnerId, requesterProfileId);
        UserProfileRelationshipEntity reciprocal = getOrCreateRelationship(requesterProfileId, primaryOwnerId);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        relationship.setConnected(true);
        relationship.setStatus("ACCEPTED");
        relationship.setUpdatedAt(now);
        relationshipRepository.save(relationship);

        reciprocal.setConnected(true);
        reciprocal.setStatus("ACCEPTED");
        reciprocal.setUpdatedAt(now);
        relationshipRepository.save(reciprocal);

        String requesterName = getProfileName(requesterProfileId);
        String ownerName = getProfileName(primaryOwnerId);

        notificationRepository.save(new NotificationEntity(
                requesterProfileId,
                "Connection Request Accepted",
                ownerName + " accepted your connection request on TrueHunt.",
                "connection_accepted"));

        notificationRepository.save(new NotificationEntity(
                primaryOwnerId,
                "New Connection Established",
                "You are now connected with " + requesterName + " on TrueHunt.",
                "connection"));

        Map<String, Object> res = new HashMap<>();
        res.put("id", requesterProfileId);
        res.put("profileId", requesterProfileId);
        res.put("connected", true);
        res.put("isConnected", true);
        res.put("isPending", false);
        res.put("status", "ACCEPTED");
        res.put("message", "Connected with " + requesterName);
        return res;
    }

    @Transactional
    public Map<String, Object> rejectConnection(String profileId, String userEmail) {
        String requesterProfileId = requireProfileId(profileId);
        String primaryOwnerId = getPrimaryOwnerUserId(userEmail);

        UserProfileRelationshipEntity relationship = getOrCreateRelationship(primaryOwnerId, requesterProfileId);
        UserProfileRelationshipEntity reciprocal = getOrCreateRelationship(requesterProfileId, primaryOwnerId);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        relationship.setConnected(false);
        relationship.setStatus("REJECTED");
        relationship.setUpdatedAt(now);
        relationshipRepository.save(relationship);

        reciprocal.setConnected(false);
        reciprocal.setStatus("REJECTED");
        reciprocal.setUpdatedAt(now);
        relationshipRepository.save(reciprocal);

        Map<String, Object> res = new HashMap<>();
        res.put("id", requesterProfileId);
        res.put("profileId", requesterProfileId);
        res.put("connected", false);
        res.put("isConnected", false);
        res.put("isPending", false);
        res.put("status", "REJECTED");
        res.put("message", "Invitation ignored");
        return res;
    }

    @Transactional
    public Map<String, Object> cancelConnectionRequest(String profileId, String userEmail) {
        String targetProfileId = requireProfileId(profileId);
        String primaryOwnerId = getPrimaryOwnerUserId(userEmail);

        UserProfileRelationshipEntity relationship = getOrCreateRelationship(primaryOwnerId, targetProfileId);
        UserProfileRelationshipEntity reciprocal = getOrCreateRelationship(targetProfileId, primaryOwnerId);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        relationship.setConnected(false);
        relationship.setStatus(null);
        relationship.setSenderUserId(null);
        relationship.setUpdatedAt(now);
        relationshipRepository.save(relationship);

        reciprocal.setConnected(false);
        reciprocal.setStatus(null);
        reciprocal.setSenderUserId(null);
        reciprocal.setUpdatedAt(now);
        relationshipRepository.save(reciprocal);

        Map<String, Object> res = new HashMap<>();
        res.put("id", targetProfileId);
        res.put("profileId", targetProfileId);
        res.put("connected", false);
        res.put("isConnected", false);
        res.put("isPending", false);
        res.put("isOutgoingPending", false);
        res.put("status", null);
        res.put("message", "Connection request withdrawn");
        return res;
    }

    @Transactional
    public Map<String, Object> toggleConnection(String profileId, String userEmail) {
        String targetProfileId = requireProfileId(profileId);
        Set<String> ownerIds = getOwnerUserIdentifiers(userEmail);
        Set<String> lowerOwnerIds = ownerIds.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        if (lowerOwnerIds.contains(targetProfileId.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("You cannot connect with your own profile");
        }

        String primaryOwnerId = getPrimaryOwnerUserId(userEmail);
        UserProfileRelationshipEntity relationship = getOrCreateRelationship(primaryOwnerId, targetProfileId);

        // 1. If currently connected -> disconnect
        if (relationship.isConnected()) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            relationship.setConnected(false);
            relationship.setStatus(null);
            relationship.setSenderUserId(null);
            relationship.setUpdatedAt(now);
            relationshipRepository.save(relationship);

            UserProfileRelationshipEntity reciprocal = getOrCreateRelationship(targetProfileId, primaryOwnerId);
            reciprocal.setConnected(false);
            reciprocal.setStatus(null);
            reciprocal.setSenderUserId(null);
            reciprocal.setUpdatedAt(now);
            relationshipRepository.save(reciprocal);

            String targetName = getProfileName(targetProfileId);
            Map<String, Object> res = new HashMap<>();
            res.put("id", targetProfileId);
            res.put("profileId", targetProfileId);
            res.put("connected", false);
            res.put("isConnected", false);
            res.put("isPending", false);
            res.put("status", null);
            res.put("message", "Disconnected from " + targetName);
            return res;
        }

        // 2. If currently pending:
        if ("PENDING".equalsIgnoreCase(relationship.getStatus())) {
            String sender = relationship.getSenderUserId() != null
                    ? relationship.getSenderUserId().toLowerCase(Locale.ROOT)
                    : "";
            if (lowerOwnerIds.contains(sender)) {
                // Sent by current user -> cancel request
                return cancelConnectionRequest(targetProfileId, userEmail);
            } else {
                // Sent by other user -> accept invitation
                return acceptConnection(targetProfileId, userEmail);
            }
        }

        // 3. Otherwise, send connection request!
        return sendConnectionRequest(targetProfileId, null, userEmail);
    }

    @Transactional
    public Map<String, Object> toggleFollow(String profileId, String userEmail) {
        String targetProfileId = requireProfileId(profileId);
        Set<String> ownerIds = getOwnerUserIdentifiers(userEmail);
        Set<String> lowerOwnerIds = ownerIds.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        if (lowerOwnerIds.contains(targetProfileId.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("You cannot follow your own profile");
        }

        String primaryOwnerId = getPrimaryOwnerUserId(userEmail);
        UserProfileRelationshipEntity relationship = getOrCreateRelationship(primaryOwnerId, targetProfileId);
        boolean newStatus = !relationship.isFollowing();
        relationship.setFollowing(newStatus);
        relationshipRepository.save(relationship);

        Map<String, Object> res = new HashMap<>();
        res.put("id", targetProfileId);
        res.put("profileId", targetProfileId);
        res.put("following", newStatus);
        res.put("isFollowing", newStatus);
        res.put("message", newStatus
                ? "Following " + getProfileName(targetProfileId)
                : "Unfollowed " + getProfileName(targetProfileId));
        return res;
    }

    private boolean isInvalidTarget(String target) {
        if (target == null || target.isBlank())
            return true;
        String lower = target.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".jpeg")
                || lower.endsWith(".webp") || lower.contains("uploads/");
    }

    private String requireProfileId(String profileId) {
        if (profileId == null || profileId.isBlank() || isInvalidTarget(profileId)) {
            throw new BadRequestException("A valid profileId is required");
        }
        return profileId.trim();
    }

    private Set<String> getOwnerUserIdentifiers(String userEmail) {
        Set<String> ids = new HashSet<>();
        String email = userEmail;
        if (email == null || email.isBlank()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                email = auth.getName();
            }
        }
        if (email != null && !email.isBlank()) {
            String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
            ids.add(cleanEmail);
            userAccountRepository.findByEmail(cleanEmail).ifPresent(acc -> {
                if (acc.getPublicUserId() != null && !acc.getPublicUserId().isBlank()) {
                    ids.add(acc.getPublicUserId());
                    ids.add(acc.getPublicUserId().toLowerCase(Locale.ROOT));
                }
                ids.add(String.valueOf(acc.getId()));
            });
            profileRepository.findByEmail(cleanEmail).ifPresent(p -> {
                if (p.getUserId() != null && !p.getUserId().isBlank()) {
                    ids.add(p.getUserId());
                    ids.add(p.getUserId().toLowerCase(Locale.ROOT));
                }
            });
            userAccountRepository.findByPublicUserId(cleanEmail).ifPresent(acc -> {
                if (acc.getEmail() != null) ids.add(acc.getEmail().toLowerCase(Locale.ROOT));
                ids.add(String.valueOf(acc.getId()));
            });
            profileRepository.findByUserId(cleanEmail).ifPresent(p -> {
                if (p.getEmail() != null) ids.add(p.getEmail().toLowerCase(Locale.ROOT));
                if (p.getUserId() != null) ids.add(p.getUserId().toLowerCase(Locale.ROOT));
            });
        }
        String primaryId = getPrimaryOwnerUserId(userEmail);
        if (primaryId != null && !primaryId.isBlank()) {
            ids.add(primaryId);
            ids.add(primaryId.toLowerCase(Locale.ROOT));
        }
        if (ids.isEmpty()) {
            ids.add("1000001");
            ids.add("me");
            ids.add("pro@truehunt.in");
            userAccountRepository.findByEmail("pro@truehunt.in").ifPresent(acc -> {
                if (acc.getPublicUserId() != null && !acc.getPublicUserId().isBlank()) {
                    ids.add(acc.getPublicUserId());
                    ids.add(acc.getPublicUserId().toLowerCase(Locale.ROOT));
                }
            });
        }
        return ids;
    }

    private String getPrimaryOwnerUserId(String userEmail) {
        String email = userEmail;
        if (email == null || email.isBlank()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                email = auth.getName();
            }
        }
        if (email == null || email.isBlank()) {
            email = "pro@truehunt.in";
        }
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        UserAccountEntity account = userAccountRepository.findByEmail(cleanEmail)
                .or(() -> userAccountRepository.findByPublicUserId(cleanEmail))
                .orElse(null);
        if (account != null) {
            return account.getPublicUserId() == null || account.getPublicUserId().isBlank()
                    ? String.valueOf(account.getId())
                    : account.getPublicUserId();
        }

        Optional<ProfileEntity> profile = profileRepository.findByEmail(cleanEmail)
                .or(() -> profileRepository.findByUserId(cleanEmail));
        if (profile.isPresent() && profile.get().getUserId() != null && !profile.get().getUserId().isBlank()) {
            return profile.get().getUserId();
        }

        if (userEmail != null && !userEmail.isBlank()) {
            return userEmail.trim();
        }

        return "1000001";
    }

    private UserProfileRelationshipEntity getOrCreateRelationship(String ownerUserId, String targetProfileId) {
        return relationshipRepository.findByOwnerUserIdAndTargetProfileId(ownerUserId, targetProfileId)
                .orElseGet(() -> {
                    UserProfileRelationshipEntity relationship = new UserProfileRelationshipEntity();
                    relationship.setOwnerUserId(ownerUserId);
                    relationship.setTargetProfileId(targetProfileId);
                    return relationship;
                });
    }

    private Map<String, Object> getProfileDto(String profileId, boolean connected, boolean following) {
        Map<String, Object> dto = new HashMap<>();
        Optional<ProfileEntity> profile = profileRepository.findByUserId(profileId);
        if (profile.isEmpty()) {
            profile = userAccountRepository.findByPublicUserId(profileId)
                    .flatMap(acc -> profileRepository.findByUserId(acc.getPublicUserId()))
                    .or(() -> userAccountRepository.findByEmail(profileId.toLowerCase(Locale.ROOT))
                            .flatMap(acc -> profileRepository.findByEmail(acc.getEmail())));
        }

        if (profile.isPresent()) {
            ProfileEntity entity = profile.get();
            String name = entity.getName() == null || entity.getName().isBlank()
                    ? "TrueHunt Member"
                    : entity.getName();
            dto.put("name", name);
            dto.put("role", entity.getHeadline() != null ? entity.getHeadline() : entity.getPrimaryCraft());
            dto.put("location", entity.getAddress() != null ? entity.getAddress() : "");
            dto.put("profileImageUrl", entity.getProfileImageUrl() != null ? entity.getProfileImageUrl() : "");
            dto.put("avatar", entity.getProfileImageUrl() != null ? entity.getProfileImageUrl() : "");
            dto.put("initials", getInitials(name));
        } else {
            Optional<NetworkProfileEntity> networkProfile = networkProfileRepository.findById(profileId);
            if (networkProfile.isPresent()) {
                dto.putAll(networkProfile.get().toDto());
            } else {
                TalentService talentService = talentServiceProvider != null ? talentServiceProvider.getIfAvailable()
                        : null;
                TalentDto talent = null;
                if (talentService != null) {
                    try {
                        talent = talentService.getById(profileId);
                    } catch (Exception ignored) {
                    }
                }
                if (talent != null) {
                    dto.put("name", talent.getName());
                    dto.put("role", talent.getRole() != null ? talent.getRole() : talent.getPrimaryCraft());
                    dto.put("location", talent.getLocation() != null ? talent.getLocation() : "");
                    dto.put("profileImageUrl", talent.getAvatar() != null ? talent.getAvatar() : "");
                    dto.put("avatar", talent.getAvatar() != null ? talent.getAvatar() : "");
                    dto.put("initials", getInitials(talent.getName()));
                } else {
                    dto.put("name", "TrueHunt Member");
                    dto.put("initials", "TH");
                    dto.put("role", "Cinema Professional");
                    dto.put("location", "");
                    dto.put("profileImageUrl", "");
                    dto.put("avatar", "");
                }
            }
        }
        dto.put("id", profileId);
        dto.put("userId", profileId);
        dto.put("isConnected", connected);
        dto.put("connected", connected);
        dto.put("isFollowing", following);
        dto.put("following", following);
        return dto;
    }

    private String getProfileName(String profileId) {
        return profileRepository.findByUserId(profileId)
                .map(ProfileEntity::getName)
                .filter(name -> name != null && !name.isBlank())
                .or(() -> networkProfileRepository.findById(profileId).map(NetworkProfileEntity::getName))
                .filter(name -> name != null && !name.isBlank())
                .or(() -> {
                    TalentService talentService = talentServiceProvider != null ? talentServiceProvider.getIfAvailable()
                            : null;
                    if (talentService != null) {
                        try {
                            TalentDto t = talentService.getById(profileId);
                            if (t != null && t.getName() != null && !t.getName().isBlank()) {
                                return Optional.of(t.getName());
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    return Optional.empty();
                })
                .orElse("TrueHunt Member");
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank())
            return "TH";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1)
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    @Transactional
    public Map<String, Object> connectTalent(ConnectionRequestDto req) {
        return connectTalent(req, null);
    }

    @Transactional
    public Map<String, Object> connectTalent(ConnectionRequestDto req, String userEmail) {
        String talentId = req.getTalentId();
        if (talentId == null || talentId.isBlank() || isInvalidTarget(talentId)) {
            throw new BadRequestException("Valid talentId is required");
        }
        String targetProfileId = talentId.trim();
        Set<String> ownerIds = getOwnerUserIdentifiers(userEmail);
        Set<String> lowerOwnerIds = ownerIds.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        if (lowerOwnerIds.contains(targetProfileId.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("You cannot connect with your own profile");
        }

        String primaryOwnerId = getPrimaryOwnerUserId(userEmail);
        UserProfileRelationshipEntity rel1 = getOrCreateRelationship(primaryOwnerId, targetProfileId);
        rel1.setConnected(true);
        rel1.setStatus("ACCEPTED");
        rel1.setUpdatedAt(java.time.LocalDateTime.now());
        relationshipRepository.save(rel1);

        UserProfileRelationshipEntity rel2 = getOrCreateRelationship(targetProfileId, primaryOwnerId);
        rel2.setConnected(true);
        rel2.setStatus("ACCEPTED");
        rel2.setUpdatedAt(java.time.LocalDateTime.now());
        relationshipRepository.save(rel2);

        String targetName = getProfileName(targetProfileId);
        String ownerName = getProfileName(primaryOwnerId);

        notificationRepository.save(new NotificationEntity(
                primaryOwnerId,
                "Connection Established",
                "You are now connected with " + targetName + " on TrueHunt.",
                "connection"));
        notificationRepository.save(new NotificationEntity(
                targetProfileId,
                "New Connection Established",
                ownerName + " connected with you on TrueHunt.",
                "connection"));

        Map<String, Object> res = new HashMap<>();
        res.put("talentId", targetProfileId);
        res.put("id", targetProfileId);
        res.put("profileId", targetProfileId);
        res.put("connected", true);
        res.put("isConnected", true);
        res.put("status", "CONNECTED");
        res.put("message", "Connected with " + targetName);
        res.put("timestamp", req.getTimestamp() != null ? req.getTimestamp() : new Date().toString());
        return res;
    }
}
