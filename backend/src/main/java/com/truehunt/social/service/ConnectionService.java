package com.truehunt.social.service;

import com.truehunt.social.dto.ConnectionRequestDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import com.truehunt.social.exception.UnauthorizedException;

@Service
public class ConnectionService {

    private final NetworkProfileRepository networkProfileRepository;
    private final NotificationRepository notificationRepository;
    private final ProfileRepository profileRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRelationshipRepository relationshipRepository;

    public ConnectionService(NetworkProfileRepository networkProfileRepository,
                             NotificationRepository notificationRepository,
                             ProfileRepository profileRepository,
                             UserAccountRepository userAccountRepository,
                             UserProfileRelationshipRepository relationshipRepository) {
        this.networkProfileRepository = networkProfileRepository;
        this.notificationRepository = notificationRepository;
        this.profileRepository = profileRepository;
        this.userAccountRepository = userAccountRepository;
        this.relationshipRepository = relationshipRepository;
    }

    public List<Map<String, Object>> getSuggestions(String userEmail) {
        String ownerUserId = getOwnerUserId(userEmail);
        Map<String, UserProfileRelationshipEntity> relationships = relationshipRepository
                .findAllByOwnerUserIdAndConnectedTrue(ownerUserId).stream()
                .collect(Collectors.toMap(
                        UserProfileRelationshipEntity::getTargetProfileId,
                        relationship -> relationship
                ));
        relationshipRepository.findAllByOwnerUserIdAndFollowingTrue(ownerUserId)
                .forEach(relationship -> relationships.put(relationship.getTargetProfileId(), relationship));

        Map<String, Map<String, Object>> profiles = new LinkedHashMap<>();
        for (NetworkProfileEntity profile : networkProfileRepository.findAll()) {
            Map<String, Object> dto = profile.toDto();
            String profileId = profile.getId();
            UserProfileRelationshipEntity relationship = relationships.get(profileId);
            dto.put("isConnected", relationship != null && relationship.isConnected());
            dto.put("connected", relationship != null && relationship.isConnected());
            dto.put("isFollowing", relationship != null && relationship.isFollowing());
            dto.put("following", relationship != null && relationship.isFollowing());
            profiles.put(profileId, dto);
        }

        for (UserProfileRelationshipEntity relationship : relationships.values()) {
            profiles.computeIfAbsent(
                    relationship.getTargetProfileId(),
                    profileId -> getProfileDto(profileId, relationship.isConnected(), relationship.isFollowing())
            );
        }
        return new ArrayList<>(profiles.values());
    }

    public List<Map<String, Object>> getConnectedProfiles(String userEmail) {
        String ownerUserId = getOwnerUserId(userEmail);
        return relationshipRepository.findAllByOwnerUserIdAndConnectedTrue(ownerUserId).stream()
                .map(relationship -> getProfileDto(
                        relationship.getTargetProfileId(),
                        true,
                        relationship.isFollowing()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> toggleConnection(String profileId, String userEmail) {
        String targetProfileId = requireProfileId(profileId);
        String ownerUserId = getOwnerUserId(userEmail);
        UserProfileRelationshipEntity relationship = getOrCreateRelationship(ownerUserId, targetProfileId);
        boolean newStatus = !relationship.isConnected();
        relationship.setConnected(newStatus);
        relationshipRepository.save(relationship);

        if (newStatus) {
            String name = getProfileName(targetProfileId);
            notificationRepository.save(new NotificationEntity(
                    ownerUserId,
                    "New Connection Established",
                    "You are now connected with " + name + " on TrueHunt.",
                    "connection"
            ));
        }

        Map<String, Object> res = new HashMap<>();
        res.put("id", targetProfileId);
        res.put("profileId", targetProfileId);
        res.put("connected", newStatus);
        res.put("isConnected", newStatus);
        res.put("message", newStatus
                ? "Connected with " + getProfileName(targetProfileId)
                : "Disconnected from " + getProfileName(targetProfileId));
        return res;
    }

    @Transactional
    public Map<String, Object> toggleFollow(String profileId, String userEmail) {
        String targetProfileId = requireProfileId(profileId);
        String ownerUserId = getOwnerUserId(userEmail);
        UserProfileRelationshipEntity relationship = getOrCreateRelationship(ownerUserId, targetProfileId);
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

    private String requireProfileId(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            throw new BadRequestException("profileId is required");
        }
        return profileId.trim();
    }

    private String getOwnerUserId(String userEmail) {
        UserAccountEntity account = userEmail == null || userEmail.isBlank()
                ? null
                : userAccountRepository.findByEmail(userEmail.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (account == null) {
            throw new UnauthorizedException("A signed-in account is required to manage network relationships.");
        }
        return account.getPublicUserId() == null || account.getPublicUserId().isBlank()
                ? String.valueOf(account.getId())
                : account.getPublicUserId();
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
                dto.put("name", "TrueHunt Member");
                dto.put("initials", "TH");
                dto.put("role", "Cinema Professional");
                dto.put("location", "");
                dto.put("profileImageUrl", "");
                dto.put("avatar", "");
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
                .orElse("TrueHunt Member");
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "TH";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    @Transactional
    public Map<String, Object> connectTalent(ConnectionRequestDto req) {
        String talentId = req.getTalentId();
        if (talentId == null || talentId.isBlank()) {
            throw new BadRequestException("talentId is required");
        }

        notificationRepository.save(new NotificationEntity(
                "me",
                "Connection Request Sent",
                "Your connection invitation was sent to talent " + talentId + (req.getMessage() != null && !req.getMessage().isBlank() ? ": \"" + req.getMessage() + "\"" : "."),
                "connection"
        ));

        Map<String, Object> res = new HashMap<>();
        res.put("talentId", talentId);
        res.put("status", "SENT");
        res.put("message", "Connection request sent successfully.");
        res.put("timestamp", req.getTimestamp() != null ? req.getTimestamp() : new Date().toString());
        return res;
    }
}
