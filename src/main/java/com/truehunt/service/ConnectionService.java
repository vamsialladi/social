package com.truehunt.service;

import com.truehunt.dto.response.ConnectionResponse;
import com.truehunt.dto.response.ProfileResponse;
import com.truehunt.exception.ConflictException;
import com.truehunt.exception.ForbiddenException;
import com.truehunt.exception.ResourceNotFoundException;
import com.truehunt.exception.ValidationException;
import com.truehunt.model.Connection;
import com.truehunt.model.Profile;
import com.truehunt.repository.ConnectionRepository;
import com.truehunt.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConnectionService {

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private NotificationService notificationService;

    public List<ProfileResponse> getSuggestions(String currentUserId) {
        List<Connection> userConnections = connectionRepository.findAllForUser(currentUserId);
        Set<String> connectedOrPendingUserIds = new HashSet<>();
        connectedOrPendingUserIds.add(currentUserId); // exclude self

        for (Connection c : userConnections) {
            if ("accepted".equalsIgnoreCase(c.getStatus()) || "pending".equalsIgnoreCase(c.getStatus())) {
                connectedOrPendingUserIds.add(c.getRequesterId());
                connectedOrPendingUserIds.add(c.getRecipientId());
            }
        }

        List<Profile> allProfiles = profileRepository.findAll();
        return allProfiles.stream()
                .filter(p -> !connectedOrPendingUserIds.contains(p.getUserId()))
                .filter(p -> !"private".equalsIgnoreCase(p.getVisibility()))
                .map(p -> profileService.toResponse(p, false, false))
                .collect(Collectors.toList());
    }

    @Transactional
    public ConnectionResponse requestConnection(String currentUserId, String recipientId) {
        if (currentUserId.equals(recipientId)) {
            throw new ValidationException("You cannot connect with yourself.");
        }

        Profile recipientProfile = profileRepository.findByUserId(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user profile not found."));

        Optional<Connection> existing = connectionRepository.findConnectionBetween(currentUserId, recipientId);
        Connection connection;

        if (existing.isPresent()) {
            connection = existing.get();
            if ("accepted".equalsIgnoreCase(connection.getStatus())) {
                throw new ConflictException("You are already connected with this user.");
            }
            if ("pending".equalsIgnoreCase(connection.getStatus())) {
                throw new ConflictException("A connection request between you is already pending.");
            }
            if ("blocked".equalsIgnoreCase(connection.getStatus())) {
                throw new ForbiddenException("Cannot connect with this user.");
            }
            // If previously rejected, re-request
            connection.setRequesterId(currentUserId);
            connection.setRecipientId(recipientId);
            connection.setStatus("pending");
            connection = connectionRepository.save(connection);
        } else {
            connection = new Connection(currentUserId, recipientId, "pending");
            connection = connectionRepository.save(connection);
        }

        // Create notification for recipient
        notificationService.createNotification(
                recipientId,
                currentUserId,
                "connection_request",
                "connection",
                connection.getId()
        );

        return toConnectionResponse(connection, currentUserId);
    }

    @Transactional
    public ConnectionResponse updateConnectionStatus(String currentUserId, String connectionId, String newStatus) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found."));

        // Only the recipient is allowed to accept or reject a pending request
        if (!connection.getRecipientId().equals(currentUserId)) {
            throw new ForbiddenException("You are not authorized to respond to this connection request.");
        }

        String status = newStatus.toLowerCase().trim();
        if (!Arrays.asList("accepted", "rejected", "blocked").contains(status)) {
            throw new ValidationException("Invalid connection status: " + status);
        }

        connection.setStatus(status);
        connection = connectionRepository.save(connection);

        if ("accepted".equals(status)) {
            // Notify the requester that connection was accepted
            notificationService.createNotification(
                    connection.getRequesterId(),
                    currentUserId,
                    "connection_accepted",
                    "connection",
                    connection.getId()
            );
        }

        return toConnectionResponse(connection, currentUserId);
    }

    public List<ConnectionResponse> getAcceptedConnections(String currentUserId) {
        List<Connection> connections = connectionRepository.findAcceptedConnections(currentUserId);
        return connections.stream()
                .map(c -> toConnectionResponse(c, currentUserId))
                .collect(Collectors.toList());
    }

    private ConnectionResponse toConnectionResponse(Connection connection, String currentUserId) {
        ConnectionResponse res = new ConnectionResponse();
        res.setId(connection.getId());
        res.setRequesterId(connection.getRequesterId());
        res.setRecipientId(connection.getRecipientId());
        res.setStatus(connection.getStatus());
        res.setCreatedAt(connection.getCreatedAt());
        res.setUpdatedAt(connection.getUpdatedAt());

        profileRepository.findByUserId(connection.getRequesterId())
                .ifPresent(p -> res.setRequesterProfile(profileService.toResponse(p, p.getUserId().equals(currentUserId), "accepted".equals(connection.getStatus()))));

        profileRepository.findByUserId(connection.getRecipientId())
                .ifPresent(p -> res.setRecipientProfile(profileService.toResponse(p, p.getUserId().equals(currentUserId), "accepted".equals(connection.getStatus()))));

        return res;
    }
}
