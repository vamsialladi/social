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
        String primaryOwnerId = getPrimaryOwnerUserId(userEmail);

        Map<String, UserProfileRelationshipEntity> relationships = new HashMap<>();
        for (String id : ownerIds) {
            relationshipRepository.findAllByOwnerUserIdAndConnectedTrue(id)
                    .forEach(r -> relationships.put(r.getTargetProfileId(), r));
            relationshipRepository.findAllByOwnerUserIdAndFollowingTrue(id)
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
            dto.put("isConnected", relationship != null && relationship.isConnected());
            dto.put("connected", relationship != null && relationship.isConnected());
            dto.put("isFollowing", relationship != null && relationship.isFollowing());
            dto.put("following", relationship != null && relationship.isFollowing());
            profiles.put(profileId, dto);
        }

        for (UserProfileRelationshipEntity relationship : relationships.values()) {
            String targetId = relationship.getTargetProfileId();
            if (targetId != null && !lowerOwnerIds.contains(targetId.toLowerCase(Locale.ROOT)) && !isInvalidTarget(targetId)) {
                profiles.computeIfAbsent(
                        targetId,
                        profileId -> getProfileDto(profileId, relationship.isConnected(), relationship.isFollowing())
                );
            }
        }
        return new ArrayList<>(profiles.values());
    }

    public List<Map<String, Object>> getConnectedProfiles(String userEmail) {
        Set<String> ownerIds = getOwnerUserIdentifiers(userEmail);
        Set<String> lowerOwnerIds = ownerIds.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        List<UserProfileRelationshipEntity> allRels = relationshipRepository.findAll();
        Map<String, Map<String, Object>> connectedMap = new LinkedHashMap<>();

        for (UserProfileRelationshipEntity rel : allRels) {
            if (!rel.isConnected()) continue;
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
    public Map<String, Object> toggleConnection(String profileId, String userEmail) {
        String targetProfileId = requireProfileId(profileId);
        Set<String> ownerIds = getOwnerUserIdentifiers(userEmail);
        Set<String> lowerOwnerIds = ownerIds.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        if (lowerOwnerIds.contains(targetProfileId.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("You cannot connect with your own profile");
        }

        String primaryOwnerId = getPrimaryOwnerUserId(userEmail);
        UserProfileRelationshipEntity relationship = getOrCreateRelationship(primaryOwnerId, targetProfileId);
        boolean newStatus = !relationship.isConnected();
        relationship.setConnected(newStatus);
        relationshipRepository.save(relationship);

        UserProfileRelationshipEntity reciprocal = getOrCreateRelationship(targetProfileId, primaryOwnerId);
        reciprocal.setConnected(newStatus);
        relationshipRepository.save(reciprocal);

        String targetName = getProfileName(targetProfileId);
        String ownerName = getProfileName(primaryOwnerId);

        if (newStatus) {
            notificationRepository.save(new NotificationEntity(
                    primaryOwnerId,
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
        if (target == null || target.isBlank()) return true;
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
                TalentService talentService = talentServiceProvider != null ? talentServiceProvider.getIfAvailable() : null;
                TalentDto talent = null;
                if (talentService != null) {
                    try {
                        talent = talentService.getById(profileId);
                    } catch (Exception ignored) {}
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
                    TalentService talentService = talentServiceProvider != null ? talentServiceProvider.getIfAvailable() : null;
                    if (talentService != null) {
                        try {
                            TalentDto t = talentService.getById(profileId);
                            if (t != null && t.getName() != null && !t.getName().isBlank()) {
                                return Optional.of(t.getName());
                            }
                        } catch (Exception ignored) {}
                    }
                    return Optional.empty();
                })
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
        relationshipRepository.save(rel1);

        UserProfileRelationshipEntity rel2 = getOrCreateRelationship(targetProfileId, primaryOwnerId);
        rel2.setConnected(true);
        relationshipRepository.save(rel2);

        String targetName = getProfileName(targetProfileId);
        String ownerName = getProfileName(primaryOwnerId);

        notificationRepository.save(new NotificationEntity(
                primaryOwnerId,
                "Connection Established",
                "You are now connected with " + targetName + " on TrueHunt.",
                "connection"
        ));
        notificationRepository.save(new NotificationEntity(
                targetProfileId,
                "New Connection Established",
                ownerName + " connected with you on TrueHunt.",
                "connection"
        ));

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
