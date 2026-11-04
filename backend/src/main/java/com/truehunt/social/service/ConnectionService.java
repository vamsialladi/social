package com.truehunt.social.service;

import com.truehunt.social.dto.ConnectionRequestDto;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.NetworkProfileEntity;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.repository.NetworkProfileRepository;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConnectionService {

    private final NetworkProfileRepository networkProfileRepository;
    private final NotificationRepository notificationRepository;
    private final ProfileRepository profileRepository;

    public ConnectionService(NetworkProfileRepository networkProfileRepository,
                             NotificationRepository notificationRepository,
                             ProfileRepository profileRepository) {
        this.networkProfileRepository = networkProfileRepository;
        this.notificationRepository = notificationRepository;
        this.profileRepository = profileRepository;
    }

    public List<Map<String, Object>> getSuggestions() {
        return networkProfileRepository.findAll().stream()
                .map(NetworkProfileEntity::toDto)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getConnectedProfiles() {
        return networkProfileRepository.findAllByConnectedTrue().stream()
                .map(NetworkProfileEntity::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> toggleConnection(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            throw new BadRequestException("profileId is required");
        }

        NetworkProfileEntity p = networkProfileRepository.findById(profileId).orElseGet(() -> {
            Optional<ProfileEntity> profOpt = profileRepository.findByUserId(profileId)
                    .or(() -> profileRepository.findByEmail(profileId));
            if (profOpt.isPresent()) {
                ProfileEntity prof = profOpt.get();
                String name = prof.getName() != null ? prof.getName() : "TrueHunt Member";
                String role = prof.getRole() != null ? prof.getRole() : prof.getPrimaryCraft();
                String loc = prof.getAddress() != null ? prof.getAddress() : "Hyderabad, Telangana";
                return networkProfileRepository.save(new NetworkProfileEntity(
                        profileId, name, getInitials(name), "active", role, loc, "1st connection", "network-avatar-coral", prof.getProfileImageUrl()
                ));
            }
            return new NetworkProfileEntity(profileId, "TrueHunt Member", "TH", "active", "Cinema Professional", "Hyderabad, India", "Connected on TrueHunt", "network-avatar-coral");
        });

        boolean newStatus = !p.isConnected();
        p.setConnected(newStatus);
        networkProfileRepository.save(p);

        if (newStatus) {
            notificationRepository.save(new NotificationEntity(
                    "me",
                    "New Connection Established",
                    "You are now connected with " + p.getName() + " on TrueHunt.",
                    "connection"
            ));
        }

        Map<String, Object> res = new HashMap<>();
        res.put("id", p.getId());
        res.put("profileId", p.getId());
        res.put("connected", newStatus);
        res.put("isConnected", newStatus);
        res.put("message", newStatus ? "Connected with " + p.getName() : "Disconnected from " + p.getName());
        return res;
    }

    @Transactional
    public Map<String, Object> toggleFollow(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            throw new BadRequestException("profileId is required");
        }

        NetworkProfileEntity p = networkProfileRepository.findById(profileId).orElseGet(() -> {
            Optional<ProfileEntity> profOpt = profileRepository.findByUserId(profileId)
                    .or(() -> profileRepository.findByEmail(profileId));
            if (profOpt.isPresent()) {
                ProfileEntity prof = profOpt.get();
                String name = prof.getName() != null ? prof.getName() : "TrueHunt Member";
                String role = prof.getRole() != null ? prof.getRole() : prof.getPrimaryCraft();
                String loc = prof.getAddress() != null ? prof.getAddress() : "Hyderabad, Telangana";
                return networkProfileRepository.save(new NetworkProfileEntity(
                        profileId, name, getInitials(name), "active", role, loc, "Following", "network-avatar-coral", prof.getProfileImageUrl()
                ));
            }
            return new NetworkProfileEntity(profileId, "TrueHunt Member", "TH", "active", "Cinema Professional", "Hyderabad, India", "Following", "network-avatar-coral");
        });

        boolean newStatus = !p.isFollowing();
        p.setFollowing(newStatus);
        networkProfileRepository.save(p);

        Map<String, Object> res = new HashMap<>();
        res.put("id", p.getId());
        res.put("profileId", p.getId());
        res.put("following", newStatus);
        res.put("isFollowing", newStatus);
        res.put("message", newStatus ? "Following " + p.getName() : "Unfollowed " + p.getName());
        return res;
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

