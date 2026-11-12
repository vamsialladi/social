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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;

    public ConnectionService(NetworkProfileRepository networkProfileRepository,
            NotificationRepository notificationRepository,
            ProfileRepository profileRepository,
            UserAccountRepository userAccountRepository,
            UserProfileRelationshipRepository relationshipRepository,
            ObjectProvider<TalentService> talentServiceProvider,
            ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider) {
        this.networkProfileRepository = networkProfileRepository;
        this.notificationRepository = notificationRepository;
        this.profileRepository = profileRepository;
        this.userAccountRepository = userAccountRepository;
        this.relationshipRepository = relationshipRepository;
        this.talentServiceProvider = talentServiceProvider;
        this.messagingTemplateProvider = messagingTemplateProvider;
    }

    private void broadcastConnectionEvent(String action, String senderId, String receiverId, boolean isConnected, boolean isPending) {
        Runnable publishEvent = () -> {
            try {
                SimpMessagingTemplate template = messagingTemplateProvider != null ? messagingTemplateProvider.getIfAvailable() : null;
                if (template != null) {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("type", "CONNECTION_UPDATED");
                    payload.put("action", action);
                    payload.put("senderId", senderId);
                    payload.put("receiverId", receiverId);
                    payload.put("connected", isConnected);
                    payload.put("isConnected", isConnected);
                    payload.put("pending", isPending);
                    payload.put("isPending", isPending);
                    payload.put("timestamp", java.time.LocalDateTime.now().toString());

                    if (senderId != null && !senderId.isBlank()) {
                        payload.put("senderProfile", getProfileDto(senderId, isConnected, false));
                    }
                    if (receiverId != null && !receiverId.isBlank()) {
                        payload.put("receiverProfile", getProfileDto(receiverId, isConnected, false));
                    }

                    template.convertAndSend("/topic/connections", payload);
                    if (senderId != null && !senderId.isBlank()) {
                        Set<String> allSenderIds = resolveAllUserIdentifiers(senderId);
                        for (String sId : allSenderIds) {
                            template.convertAndSend("/topic/user." + sId.trim().toLowerCase(Locale.ROOT) + ".connections", payload);
                        }
                    }
                    if (receiverId != null && !receiverId.isBlank()) {
                        Set<String> allReceiverIds = resolveAllUserIdentifiers(receiverId);
                        for (String rId : allReceiverIds) {
                            template.convertAndSend("/topic/user." + rId.trim().toLowerCase(Locale.ROOT) + ".connections", payload);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishEvent.run();
                }
            });
        } else {
            publishEvent.run();
        }
    }

    public Set<String> resolveAllUserIdentifiers(String identifier) {
        Set<String> ids = new LinkedHashSet<>();
        if (identifier == null || identifier.isBlank()) {
            return ids;
        }
        String clean = identifier.trim();
        ids.add(clean);
        ids.add(clean.toLowerCase(Locale.ROOT));
        ids.add(clean.toUpperCase(Locale.ROOT));
        try {
            userAccountRepository.findByEmail(clean).ifPresent(acc -> {
                if (acc.getPublicUserId() != null) {
                    ids.add(acc.getPublicUserId());
                    ids.add(acc.getPublicUserId().toLowerCase(Locale.ROOT));
                }
                ids.add(String.valueOf(acc.getId()));
                if (acc.getEmail() != null) ids.add(acc.getEmail().toLowerCase(Locale.ROOT));
            });
            userAccountRepository.findByPublicUserId(clean).ifPresent(acc -> {
                if (acc.getEmail() != null) ids.add(acc.getEmail().toLowerCase(Locale.ROOT));
                ids.add(String.valueOf(acc.getId()));
            });
            profileRepository.findByEmail(clean).ifPresent(p -> {
                if (p.getUserId() != null) {
                    ids.add(p.getUserId());
                    ids.add(p.getUserId().toLowerCase(Locale.ROOT));
                }
                if (p.getEmail() != null) ids.add(p.getEmail().toLowerCase(Locale.ROOT));
                if (p.getId() != null) ids.add(String.valueOf(p.getId()));
            });
            profileRepository.findByUserId(clean).ifPresent(p -> {
                if (p.getEmail() != null) ids.add(p.getEmail().toLowerCase(Locale.ROOT));
                if (p.getUserId() != null) ids.add(p.getUserId().toLowerCase(Locale.ROOT));
                if (p.getId() != null) ids.add(String.valueOf(p.getId()));
            });
        } catch (Exception ignored) {
        }
        return ids;
    }

    private UserProfileRelationshipEntity findRelationshipBetween(Set<String> user1Ids, Set<String> user2Ids, List<UserProfileRelationshipEntity> allRels) {
        Set<String> lower1 = user1Ids.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        Set<String> lower2 = user2Ids.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        for (UserProfileRelationshipEntity rel : allRels) {
            if (rel == null) continue;
            String owner = rel.getOwnerUserId() != null ? rel.getOwnerUserId().trim().toLowerCase(Locale.ROOT) : "";
            String target = rel.getTargetProfileId() != null ? rel.getTargetProfileId().trim().toLowerCase(Locale.ROOT) : "";
            if ((lower1.contains(owner) && lower2.contains(target)) || (lower2.contains(owner) && lower1.contains(target))) {
                return rel;
            }
        }
        return null;
    }

    private void updateAllRelationshipsBetween(Set<String> user1Ids, Set<String> user2Ids, Boolean connected, String status, String senderUserId, List<UserProfileRelationshipEntity> allRels) {
        Set<String> lower1 = user1Ids.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        Set<String> lower2 = user2Ids.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        String canonicalUser1Id = user1Ids.iterator().next();
        String canonicalUser2Id = user2Ids.iterator().next();
        boolean foundAny = false;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (UserProfileRelationshipEntity rel : allRels) {
            if (rel == null) continue;
            String owner = rel.getOwnerUserId() != null ? rel.getOwnerUserId().trim().toLowerCase(Locale.ROOT) : "";
            String target = rel.getTargetProfileId() != null ? rel.getTargetProfileId().trim().toLowerCase(Locale.ROOT) : "";
            boolean user1IsOwner = lower1.contains(owner) && lower2.contains(target);
            boolean user2IsOwner = lower2.contains(owner) && lower1.contains(target);
            if (user1IsOwner || user2IsOwner) {
                rel.setOwnerUserId(user1IsOwner ? canonicalUser1Id : canonicalUser2Id);
                rel.setTargetProfileId(user1IsOwner ? canonicalUser2Id : canonicalUser1Id);
                if (connected != null) rel.setConnected(connected);
                rel.setStatus(status);
                rel.setSenderUserId(senderUserId);
                rel.setUpdatedAt(now);
                relationshipRepository.save(rel);
                foundAny = true;
            }
        }
        if (!foundAny && !user1Ids.isEmpty() && !user2Ids.isEmpty()) {
            String p1 = user1Ids.iterator().next();
            String p2 = user2Ids.iterator().next();
            UserProfileRelationshipEntity r1 = new UserProfileRelationshipEntity();
            r1.setOwnerUserId(p1);
            r1.setTargetProfileId(p2);
            r1.setConnected(connected != null ? connected : false);
            r1.setStatus(status);
            r1.setSenderUserId(senderUserId);
            r1.setCreatedAt(now);
            r1.setUpdatedAt(now);
            relationshipRepository.save(r1);

            UserProfileRelationshipEntity r2 = new UserProfileRelationshipEntity();
            r2.setOwnerUserId(p2);
            r2.setTargetProfileId(p1);
            r2.setConnected(connected != null ? connected : false);
            r2.setStatus(status);
            r2.setSenderUserId(senderUserId);
            r2.setCreatedAt(now);
            r2.setUpdatedAt(now);
            relationshipRepository.save(r2);
        }
    }

    public List<Map<String, Object>> getSuggestions(String userEmail) {
        Set<String> ownerIds = getOwnerUserIdentifiers(userEmail);
        Set<String> lowerOwnerIds = ownerIds.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        List<UserProfileRelationshipEntity> allRels = relationshipRepository.findAll();

        Map<String, Map<String, Object>> profiles = new LinkedHashMap<>();
        for (NetworkProfileEntity profile : networkProfileRepository.findAll()) {
            if (profile.getId() != null && lowerOwnerIds.contains(profile.getId().toLowerCase(Locale.ROOT))) {
                continue;
            }
            Map<String, Object> dto = profile.toDto();
            String profileId = profile.getId();
            Set<String> targetIds = resolveAllUserIdentifiers(profileId);
            UserProfileRelationshipEntity relationship = findRelationshipBetween(ownerIds, targetIds, allRels);
            boolean isConnected = relationship != null && (relationship.isConnected() || "ACCEPTED".equalsIgnoreCase(relationship.getStatus()));
            boolean isFollowing = relationship != null && relationship.isFollowing();
            boolean isPending = relationship != null && !isConnected && "PENDING".equalsIgnoreCase(relationship.getStatus());
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

        for (UserProfileRelationshipEntity relationship : allRels) {
            String targetId = relationship.getTargetProfileId();
            String owner = relationship.getOwnerUserId();
            String candidateId = null;
            if (targetId != null && !lowerOwnerIds.contains(targetId.toLowerCase(Locale.ROOT)) && !isInvalidTarget(targetId)) {
                candidateId = targetId;
            } else if (owner != null && !lowerOwnerIds.contains(owner.toLowerCase(Locale.ROOT)) && !isInvalidTarget(owner)) {
                candidateId = owner;
            }
            if (candidateId != null) {
                final String finalCandidateId = candidateId;
                profiles.computeIfAbsent(
                        finalCandidateId,
                        profileId -> {
                            Set<String> targetIds = resolveAllUserIdentifiers(finalCandidateId);
                            UserProfileRelationshipEntity rel = findRelationshipBetween(ownerIds, targetIds, allRels);
                            boolean isConn = rel != null && (rel.isConnected() || "ACCEPTED".equalsIgnoreCase(rel.getStatus()));
                            boolean isFollow = rel != null && rel.isFollowing();
                            Map<String, Object> dto = getProfileDto(finalCandidateId, isConn, isFollow);
                            boolean isPend = rel != null && !isConn && "PENDING".equalsIgnoreCase(rel.getStatus());
                            String sender = rel != null && rel.getSenderUserId() != null
                                    ? rel.getSenderUserId().toLowerCase(Locale.ROOT)
                                    : "";
                            boolean isOutPending = isPend && lowerOwnerIds.contains(sender);
                            boolean isInPending = isPend && !isOutPending;
                            dto.put("isConnected", isConn);
                            dto.put("connected", isConn);
                            dto.put("isFollowing", isFollow);
                            dto.put("following", isFollow);
                            dto.put("isPending", isPend);
                            dto.put("isOutgoingPending", isOutPending);
                            dto.put("isIncomingPending", isInPending);
                            dto.put("connectionStatus", isConn ? "CONNECTED"
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
            if (rel == null || rel.isConnected() || !"PENDING".equalsIgnoreCase(rel.getStatus())) {
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

            String key = senderProfileId.toLowerCase(Locale.ROOT);
            if (!invitationMap.containsKey(key)) {
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
                invitationMap.put(key, inv);
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
            if (!rel.isConnected() && !"ACCEPTED".equalsIgnoreCase(rel.getStatus()))
                continue;
            String owner = rel.getOwnerUserId() != null ? rel.getOwnerUserId().trim() : "";
            String target = rel.getTargetProfileId() != null ? rel.getTargetProfileId().trim() : "";

            if (lowerOwnerIds.contains(owner.toLowerCase(Locale.ROOT))) {
                if (!lowerOwnerIds.contains(target.toLowerCase(Locale.ROOT)) && !isInvalidTarget(target)) {
                    connectedMap.putIfAbsent(target.toLowerCase(Locale.ROOT), getProfileDto(target, true, rel.isFollowing()));
                }
            } else if (lowerOwnerIds.contains(target.toLowerCase(Locale.ROOT))) {
                if (!lowerOwnerIds.contains(owner.toLowerCase(Locale.ROOT)) && !isInvalidTarget(owner)) {
                    connectedMap.putIfAbsent(owner.toLowerCase(Locale.ROOT), getProfileDto(owner, true, rel.isFollowing()));
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
        Set<String> targetIds = resolveAllUserIdentifiers(targetProfileId);

        if (lowerOwnerIds.contains(targetProfileId.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("You cannot connect with your own profile");
        }

        String primaryOwnerId = getPrimaryOwnerUserId(userEmail);
        List<UserProfileRelationshipEntity> allRels = relationshipRepository.findAll();
        UserProfileRelationshipEntity existingRel = findRelationshipBetween(ownerIds, targetIds, allRels);

        if (existingRel != null && (existingRel.isConnected() || "ACCEPTED".equalsIgnoreCase(existingRel.getStatus()))) {
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
        if (existingRel != null && "PENDING".equalsIgnoreCase(existingRel.getStatus())
                && existingRel.getSenderUserId() != null
                && !lowerOwnerIds.contains(existingRel.getSenderUserId().toLowerCase(Locale.ROOT))) {
            return acceptConnection(targetProfileId, userEmail);
        }

        updateAllRelationshipsBetween(ownerIds, targetIds, false, "PENDING", primaryOwnerId, allRels);

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

        broadcastConnectionEvent("REQUEST_SENT", primaryOwnerId, targetProfileId, false, true);

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
        Set<String> requesterIds = resolveAllUserIdentifiers(requesterProfileId);

        if (lowerOwnerIds.contains(requesterProfileId.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("You cannot connect with your own profile");
        }

        String primaryOwnerId = getPrimaryOwnerUserId(userEmail);
        List<UserProfileRelationshipEntity> allRels = relationshipRepository.findAll();
        updateAllRelationshipsBetween(ownerIds, requesterIds, true, "ACCEPTED", requesterProfileId, allRels);

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

        broadcastConnectionEvent("REQUEST_ACCEPTED", requesterProfileId, primaryOwnerId, true, false);

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
        Set<String> ownerIds = getOwnerUserIdentifiers(userEmail);
        Set<String> requesterIds = resolveAllUserIdentifiers(requesterProfileId);

        List<UserProfileRelationshipEntity> allRels = relationshipRepository.findAll();
        updateAllRelationshipsBetween(ownerIds, requesterIds, false, "REJECTED", null, allRels);

        broadcastConnectionEvent("REQUEST_REJECTED", requesterProfileId, primaryOwnerId, false, false);

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
        Set<String> ownerIds = getOwnerUserIdentifiers(userEmail);
        Set<String> targetIds = resolveAllUserIdentifiers(targetProfileId);

        List<UserProfileRelationshipEntity> allRels = relationshipRepository.findAll();
        updateAllRelationshipsBetween(ownerIds, targetIds, false, null, null, allRels);

        broadcastConnectionEvent("REQUEST_CANCELLED", primaryOwnerId, targetProfileId, false, false);

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
        Set<String> targetIds = resolveAllUserIdentifiers(targetProfileId);

        if (lowerOwnerIds.contains(targetProfileId.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("You cannot connect with your own profile");
        }

        String primaryOwnerId = getPrimaryOwnerUserId(userEmail);
        List<UserProfileRelationshipEntity> allRels = relationshipRepository.findAll();
        UserProfileRelationshipEntity relationship = findRelationshipBetween(ownerIds, targetIds, allRels);

        // 1. If currently connected -> disconnect
        if (relationship != null && (relationship.isConnected() || "ACCEPTED".equalsIgnoreCase(relationship.getStatus()))) {
            updateAllRelationshipsBetween(ownerIds, targetIds, false, null, null, allRels);
            broadcastConnectionEvent("DISCONNECTED", primaryOwnerId, targetProfileId, false, false);

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
        if (relationship != null && "PENDING".equalsIgnoreCase(relationship.getStatus())) {
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
        Set<String> ids = new LinkedHashSet<>();
        if (userEmail != null && !userEmail.isBlank()) {
            ids.addAll(resolveAllUserIdentifiers(userEmail));
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            ids.addAll(resolveAllUserIdentifiers(auth.getName()));
        }
        String primaryId = getPrimaryOwnerUserId(userEmail);
        if (primaryId != null && !primaryId.isBlank()) {
            ids.addAll(resolveAllUserIdentifiers(primaryId));
        }
        if (ids.isEmpty()) {
            ids.addAll(resolveAllUserIdentifiers("pro@truehunt.in"));
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
        String cleanEmail = email.trim();
        String lowerEmail = cleanEmail.toLowerCase(Locale.ROOT);
        String upperEmail = cleanEmail.toUpperCase(Locale.ROOT);

        UserAccountEntity account = userAccountRepository.findByEmail(cleanEmail)
                .or(() -> userAccountRepository.findByEmail(lowerEmail))
                .or(() -> userAccountRepository.findByPublicUserId(cleanEmail))
                .or(() -> userAccountRepository.findByPublicUserId(upperEmail))
                .or(() -> userAccountRepository.findByPublicUserId(lowerEmail))
                .orElse(null);
        if (account != null) {
            return account.getPublicUserId() == null || account.getPublicUserId().isBlank()
                    ? String.valueOf(account.getId())
                    : account.getPublicUserId();
        }

        Optional<ProfileEntity> profile = profileRepository.findByEmail(cleanEmail)
                .or(() -> profileRepository.findByEmail(lowerEmail))
                .or(() -> profileRepository.findByUserId(cleanEmail))
                .or(() -> profileRepository.findByUserId(upperEmail))
                .or(() -> profileRepository.findByUserId(lowerEmail));
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
        String cleanId = profileId != null ? profileId.trim() : "";
        String upperId = cleanId.toUpperCase(Locale.ROOT);
        String lowerId = cleanId.toLowerCase(Locale.ROOT);
        String canonicalId = cleanId;

        Optional<ProfileEntity> profile = profileRepository.findByUserId(cleanId)
                .or(() -> profileRepository.findByUserId(upperId))
                .or(() -> profileRepository.findByUserId(lowerId));
        if (profile.isEmpty()) {
            profile = findUserAccountByIdentifier(cleanId)
                    .flatMap(account -> profileRepository.findByUserId(account.getPublicUserId())
                            .or(() -> profileRepository.findByEmail(account.getEmail())));
        }

        if (profile.isPresent()) {
            ProfileEntity entity = profile.get();
            Optional<UserAccountEntity> linkedAccount = findUserAccountByIdentifier(entity.getEmail())
                    .or(() -> findUserAccountByIdentifier(entity.getUserId()))
                    .or(() -> findUserAccountByIdentifier(cleanId));
            canonicalId = linkedAccount
                    .map(UserAccountEntity::getPublicUserId)
                    .filter(id -> id != null && !id.isBlank())
                    .orElse(entity.getUserId() == null || entity.getUserId().isBlank()
                            ? cleanId
                            : entity.getUserId());
            String name = entity.getName();
            if (isGenericProfileName(name)) {
                name = linkedAccount
                        .map(UserAccountEntity::getName)
                        .filter(accountName -> accountName != null
                                && !accountName.isBlank()
                                && !isGenericProfileName(accountName))
                        .orElse(name);
            }
            if (isGenericProfileName(name)) {
                name = "TrueHunt Member";
            }
            dto.put("name", name);
            dto.put("role", entity.getHeadline() != null ? entity.getHeadline() : (entity.getPrimaryCraft() != null ? entity.getPrimaryCraft() : "Cinema Professional"));
            dto.put("location", entity.getAddress() != null ? entity.getAddress() : "");
            dto.put("profileImageUrl", entity.getProfileImageUrl() != null ? entity.getProfileImageUrl() : "");
            dto.put("avatar", entity.getProfileImageUrl() != null ? entity.getProfileImageUrl() : "");
            dto.put("initials", getInitials(name));
        } else {
            // Check UserAccountEntity if registered user without full profile
            Optional<UserAccountEntity> account = findUserAccountByIdentifier(cleanId);
            if (account.isPresent()) {
                UserAccountEntity acc = account.get();
                if (acc.getPublicUserId() != null && !acc.getPublicUserId().isBlank()) {
                    canonicalId = acc.getPublicUserId();
                }
                String accName = acc.getName() != null && !acc.getName().isBlank() ? acc.getName() : "TrueHunt Member";
                dto.put("name", accName);
                dto.put("role", acc.getRole() != null ? acc.getRole() : (acc.getCraft() != null ? acc.getCraft() : "Cinema Professional"));
                dto.put("location", acc.getHeadquarters() != null ? acc.getHeadquarters() : "");
                dto.put("profileImageUrl", "");
                dto.put("avatar", "");
                dto.put("initials", getInitials(accName));
            } else {
                Optional<NetworkProfileEntity> networkProfile = networkProfileRepository.findById(cleanId)
                        .or(() -> networkProfileRepository.findById(upperId))
                        .or(() -> networkProfileRepository.findById(lowerId));
                if (networkProfile.isPresent()) {
                    dto.putAll(networkProfile.get().toDto());
                } else {
                    TalentService talentService = talentServiceProvider != null ? talentServiceProvider.getIfAvailable() : null;
                    TalentDto talent = null;
                    if (talentService != null) {
                        try {
                            talent = talentService.getById(cleanId);
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
        }
        dto.put("id", canonicalId);
        dto.put("userId", canonicalId);
        dto.put("isConnected", connected);
        dto.put("connected", connected);
        dto.put("isFollowing", following);
        dto.put("following", following);
        return dto;
    }

    private String getProfileName(String profileId) {
        if (profileId == null || profileId.isBlank()) return "TrueHunt Member";
        String cleanId = profileId.trim();
        String upperId = cleanId.toUpperCase(Locale.ROOT);
        String lowerId = cleanId.toLowerCase(Locale.ROOT);

        return profileRepository.findByUserId(cleanId)
                .or(() -> profileRepository.findByUserId(upperId))
                .or(() -> profileRepository.findByUserId(lowerId))
                .map(ProfileEntity::getName)
                .filter(name -> name != null && !name.isBlank() && !isGenericProfileName(name))
                .or(() -> findUserAccountByIdentifier(cleanId).map(UserAccountEntity::getName))
                .filter(name -> name != null && !name.isBlank() && !isGenericProfileName(name))
                .or(() -> networkProfileRepository.findById(cleanId).map(NetworkProfileEntity::getName))
                .filter(name -> name != null && !name.isBlank())
                .or(() -> {
                    TalentService talentService = talentServiceProvider != null ? talentServiceProvider.getIfAvailable()
                            : null;
                    if (talentService != null) {
                        try {
                            TalentDto t = talentService.getById(cleanId);
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

    private Optional<UserAccountEntity> findUserAccountByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        String cleanId = identifier.trim();
        Optional<UserAccountEntity> account = userAccountRepository.findByPublicUserId(cleanId)
                .or(() -> userAccountRepository.findByEmail(cleanId));
        if (account.isPresent()) {
            return account;
        }

        try {
            return userAccountRepository.findById(Long.parseLong(cleanId));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private boolean isGenericProfileName(String name) {
        if (name == null || name.isBlank()) {
            return true;
        }
        String normalizedName = name.trim().toLowerCase(Locale.ROOT);
        return "truehunt member".equals(normalizedName)
                || "cinema professional".equals(normalizedName)
                || "member".equals(normalizedName);
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
        Set<String> targetIds = resolveAllUserIdentifiers(targetProfileId);
        List<UserProfileRelationshipEntity> allRels = relationshipRepository.findAll();
        updateAllRelationshipsBetween(ownerIds, targetIds, true, "ACCEPTED", targetProfileId, allRels);

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

        broadcastConnectionEvent("REQUEST_ACCEPTED", targetProfileId, primaryOwnerId, true, false);

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
