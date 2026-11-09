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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
            if (profile.getId() != null && profile.getId().equalsIgnoreCase(ownerUserId)) {
                continue;
            }
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
            if (relationship.getTargetProfileId() != null && !relationship.getTargetProfileId().equalsIgnoreCase(ownerUserId)) {
                profiles.computeIfAbsent(
                        relationship.getTargetProfileId(),
                        profileId -> getProfileDto(profileId, relationship.isConnected(), relationship.isFollowing())
                );
            }
        }
        return new ArrayList<>(profiles.values());
    }

    public List<Map<String, Object>> getConnectedProfiles(String userEmail) {
        String ownerUserId = getOwnerUserId(userEmail);
        List<UserProfileRelationshipEntity> allRels = relationshipRepository
                .findAllByOwnerUserIdOrTargetProfileId(ownerUserId, ownerUserId);

        Map<String, Map<String, Object>> connectedMap = new LinkedHashMap<>();
        for (UserProfileRelationshipEntity rel : allRels) {
            if (!rel.isConnected()) continue;
            String partnerId = ownerUserId.equalsIgnoreCase(rel.getOwnerUserId())
                    ? rel.getTargetProfileId()
                    : rel.getOwnerUserId();
            if (partnerId != null && !partnerId.equalsIgnoreCase(ownerUserId)) {
                connectedMap.putIfAbsent(partnerId, getProfileDto(partnerId, true, rel.isFollowing()));
            }
        }
        return new ArrayList<>(connectedMap.values());
    }

    @Transactional
    public Map<String, Object> toggleConnection(String profileId, String userEmail) {
        String targetProfileId = requireProfileId(profileId);
        String ownerUserId = getOwnerUserId(userEmail);
        if (ownerUserId.equalsIgnoreCase(targetProfileId)) {
            throw new BadRequestException("You cannot connect with your own profile");
        }

        UserProfileRelationshipEntity relationship = getOrCreateRelationship(ownerUserId, targetProfileId);
        boolean newStatus = !relationship.isConnected();
        relationship.setConnected(newStatus);
        relationshipRepository.save(relationship);

        UserProfileRelationshipEntity reciprocal = getOrCreateRelationship(targetProfileId, ownerUserId);
        reciprocal.setConnected(newStatus);
        relationshipRepository.save(reciprocal);

        String targetName = getProfileName(targetProfileId);
        String ownerName = getProfileName(ownerUserId);

        if (newStatus) {
            notificationRepository.save(new NotificationEntity(
                    ownerUserId,
                    "New Connection Established",
                    "You are now connected with " + targetName + " on TrueHunt.",
                    "connection"
            ));
            notificationRepository.save(new NotificationEntity(
                    targetProfileId,
                    "New Connection Established",
                    ownerName + " connected with you on TrueHunt.",
                    "connection"
            ));
        }

        Map<String, Object> res = new HashMap<>();
        res.put("id", targetProfileId);
        res.put("profileId", targetProfileId);
        res.put("connected", newStatus);
        res.put("isConnected", newStatus);
        res.put("message", newStatus
                ? "Connected with " + targetName
                : "Disconnected from " + targetName);
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
        UserAccountEntity account = userAccountRepository.findByEmail(cleanEmail).orElse(null);
        if (account != null) {
            return account.getPublicUserId() == null || account.getPublicUserId().isBlank()
                    ? String.valueOf(account.getId())
                    : account.getPublicUserId();
        }

        Optional<ProfileEntity> profile = profileRepository.findByEmail(cleanEmail);
        if (profile.isPresent() && profile.get().getUserId() != null && !profile.get().getUserId().isBlank()) {
            return profile.get().getUserId();
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
        return connectTalent(req, null);
    }

    @Transactional
    public Map<String, Object> connectTalent(ConnectionRequestDto req, String userEmail) {
        String talentId = req.getTalentId();
        if (talentId == null || talentId.isBlank()) {
            throw new BadRequestException("talentId is required");
        }
        String ownerUserId = getOwnerUserId(userEmail);
        if (ownerUserId.equalsIgnoreCase(talentId.trim())) {
            throw new BadRequestException("You cannot connect with your own profile");
        }

        UserProfileRelationshipEntity rel1 = getOrCreateRelationship(ownerUserId, talentId.trim());
        rel1.setConnected(true);
        relationshipRepository.save(rel1);

        UserProfileRelationshipEntity rel2 = getOrCreateRelationship(talentId.trim(), ownerUserId);
        rel2.setConnected(true);
        relationshipRepository.save(rel2);

        String targetName = getProfileName(talentId.trim());
        String ownerName = getProfileName(ownerUserId);

        notificationRepository.save(new NotificationEntity(
                ownerUserId,
                "Connection Established",
                "You are now connected with " + targetName + " on TrueHunt.",
                "connection"
        ));
        notificationRepository.save(new NotificationEntity(
                talentId.trim(),
                "New Connection Established",
                ownerName + " connected with you on TrueHunt.",
                "connection"
        ));

        Map<String, Object> res = new HashMap<>();
        res.put("talentId", talentId);
        res.put("id", talentId);
        res.put("profileId", talentId);
        res.put("connected", true);
        res.put("isConnected", true);
        res.put("status", "CONNECTED");
        res.put("message", "Connected with " + targetName);
        res.put("timestamp", req.getTimestamp() != null ? req.getTimestamp() : new Date().toString());
        return res;
    }
}
