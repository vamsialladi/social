package com.truehunt.social.service;

import com.truehunt.social.dto.ConnectionRequestDto;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.NetworkProfileEntity;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.repository.NetworkProfileRepository;
import com.truehunt.social.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConnectionService {

    private final NetworkProfileRepository networkProfileRepository;
    private final NotificationRepository notificationRepository;

    public ConnectionService(NetworkProfileRepository networkProfileRepository, NotificationRepository notificationRepository) {
        this.networkProfileRepository = networkProfileRepository;
        this.notificationRepository = notificationRepository;
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

        NetworkProfileEntity p = networkProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Network Profile", "id", profileId));

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
        res.put("profileId", p.getId());
        res.put("connected", newStatus);
        res.put("message", newStatus ? "Connected with " + p.getName() : "Disconnected from " + p.getName());
        return res;
    }

    @Transactional
    public Map<String, Object> toggleFollow(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            throw new BadRequestException("profileId is required");
        }

        NetworkProfileEntity p = networkProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Network Profile", "id", profileId));

        boolean newStatus = !p.isFollowing();
        p.setFollowing(newStatus);
        networkProfileRepository.save(p);

        Map<String, Object> res = new HashMap<>();
        res.put("profileId", p.getId());
        res.put("following", newStatus);
        res.put("message", newStatus ? "Following " + p.getName() : "Unfollowed " + p.getName());
        return res;
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

