package com.social.service.impl;

import com.social.dto.request.SendConnectionRequest;
import com.social.dto.request.UpdateConnectionStatusRequest;
import com.social.dto.response.ConnectionResponse;
import com.social.dto.response.UserProfileResponse;
import com.social.entity.Connection;
import com.social.entity.ConnectionStatus;
import com.social.entity.User;
import com.social.exception.BadRequestException;
import com.social.exception.ConflictException;
import com.social.exception.ForbiddenException;
import com.social.exception.ResourceNotFoundException;
import com.social.repository.ConnectionRepository;
import com.social.repository.UserRepository;
import com.social.service.ConnectionService;
import com.social.service.NotificationService;
import com.social.service.UserService;
import com.social.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionServiceImpl implements ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getSuggestions(String currentUserId, int limit) {
        int sanitizedLimit = limit <= 0 ? 20 : Math.min(limit, 50);

        // Exclude self
        Set<String> excludedUserIds = new HashSet<>();
        excludedUserIds.add(currentUserId);

        // Exclude all users with any existing relationship (pending, accepted, blocked, etc.)
        List<String> connectedIds = connectionRepository.findAllConnectedUserIds(currentUserId);
        excludedUserIds.addAll(connectedIds);

        List<User> eligibleUsers;
        if (excludedUserIds.isEmpty()) {
            eligibleUsers = userRepository.findSuggestionsExcludingSelf(currentUserId, PageRequest.of(0, sanitizedLimit));
        } else {
            eligibleUsers = userRepository.findSuggestionsExcludingIds(excludedUserIds, PageRequest.of(0, sanitizedLimit));
        }

        return eligibleUsers.stream()
                .map(user -> userService.getUserProfile(user.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConnectionResponse sendConnectionRequest(String currentUserId, SendConnectionRequest request) {
        String rawRecipientId = request.getRecipientId();
        if (rawRecipientId == null || rawRecipientId.trim().isEmpty()) {
            throw new BadRequestException("Recipient ID is required");
        }
        final String recipientId = rawRecipientId.trim();

        // Rule: No self-connections
        if (currentUserId.equals(recipientId)) {
            throw new BadRequestException("You cannot send a connection request to yourself");
        }

        // Validate recipient user exists
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with ID: " + recipientId));

        // Get requester user
        User requester = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found with ID: " + currentUserId));

        // Check if an existing connection pair exists
        Optional<Connection> existingOpt = connectionRepository.findConnectionBetween(currentUserId, recipientId);
        if (existingOpt.isPresent()) {
            Connection existing = existingOpt.get();
            if (existing.getStatus() == ConnectionStatus.BLOCKED) {
                // Rule: Blocked users must not see or contact the blocker
                throw new ForbiddenException("Cannot send connection request to this user");
            }
            if (existing.getStatus() == ConnectionStatus.ACCEPTED) {
                throw new ConflictException("You are already connected with this user");
            }
            if (existing.getStatus() == ConnectionStatus.PENDING) {
                throw new ConflictException("A pending connection request already exists between these users");
            }

            // If previously rejected, allow re-requesting by updating the connection
            existing.setRequesterId(currentUserId);
            existing.setRecipientId(recipientId);
            existing.setStatus(ConnectionStatus.PENDING);
            existing.setUpdatedAt(Instant.now());
            Connection updated = connectionRepository.save(existing);

            // Create notification for recipient (M8)
            notificationService.sendNotification(
                    recipientId,
                    currentUserId,
                    "CONNECTION_REQUEST",
                    requester.getName() + " sent you a connection request",
                    updated.getId()
            );

            log.info("Re-activated connection request {} from {} to {}", updated.getId(), currentUserId, recipientId);
            return mapToResponse(updated, requester, recipient);
        }

        // Create new pending connection
        Connection connection = Connection.builder()
                .id(IdGenerator.nextObjectId())
                .requesterId(currentUserId)
                .recipientId(recipientId)
                .status(ConnectionStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Connection saved = connectionRepository.save(connection);

        // Rule: Creates a notification for the recipient (M8)
        notificationService.sendNotification(
                recipientId,
                currentUserId,
                "CONNECTION_REQUEST",
                requester.getName() + " sent you a connection request",
                saved.getId()
        );

        log.info("Sent connection request {} from {} to {}", saved.getId(), currentUserId, recipientId);
        return mapToResponse(saved, requester, recipient);
    }

    @Override
    @Transactional
    public ConnectionResponse updateConnectionStatus(String currentUserId, String connectionId, UpdateConnectionStatusRequest request) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found with ID: " + connectionId));

        // Rule: Only the recipient can accept or reject
        if (!connection.getRecipientId().equals(currentUserId)) {
            throw new ForbiddenException("Only the recipient of this connection request can accept or reject it");
        }

        ConnectionStatus targetStatus = request.getStatus();
        if (targetStatus == ConnectionStatus.PENDING) {
            throw new BadRequestException("Cannot change status back to pending");
        }

        connection.setStatus(targetStatus);
        connection.setUpdatedAt(Instant.now());
        Connection updated = connectionRepository.save(connection);

        User recipient = userRepository.findById(currentUserId).orElse(null);
        User requester = userRepository.findById(connection.getRequesterId()).orElse(null);

        // If accepted, notify the requester
        if (targetStatus == ConnectionStatus.ACCEPTED && recipient != null) {
            notificationService.sendNotification(
                    connection.getRequesterId(),
                    currentUserId,
                    "CONNECTION_ACCEPTED",
                    recipient.getName() + " accepted your connection request",
                    connection.getId()
            );
        }

        log.info("Updated connection {} status to {} by recipient {}", connectionId, targetStatus, currentUserId);
        return mapToResponse(updated, requester, recipient);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectionResponse> getAcceptedConnections(String currentUserId) {
        List<Connection> connections = connectionRepository.findAcceptedConnectionsForUser(currentUserId);
        return connections.stream()
                .map(conn -> enrichConnectionResponse(conn, currentUserId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectionResponse getConnectionById(String currentUserId, String connectionId) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found with ID: " + connectionId));

        if (!connection.getRequesterId().equals(currentUserId) && !connection.getRecipientId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to view this connection");
        }

        return enrichConnectionResponse(connection, currentUserId);
    }

    private ConnectionResponse enrichConnectionResponse(Connection conn, String currentUserId) {
        User requester = userRepository.findById(conn.getRequesterId()).orElse(null);
        User recipient = userRepository.findById(conn.getRecipientId()).orElse(null);

        ConnectionResponse response = mapToResponse(conn, requester, recipient);

        // Set connectedUser convenience field
        if (conn.getRequesterId().equals(currentUserId)) {
            response.setConnectedUser(response.getRecipient());
        } else {
            response.setConnectedUser(response.getRequester());
        }

        return response;
    }

    private ConnectionResponse mapToResponse(Connection conn, User requester, User recipient) {
        return ConnectionResponse.builder()
                .id(conn.getId())
                .mongoId(conn.getId())
                .requesterId(conn.getRequesterId())
                .recipientId(conn.getRecipientId())
                .status(conn.getStatus())
                .createdAt(conn.getCreatedAt())
                .updatedAt(conn.getUpdatedAt())
                .requester(requester != null ? userService.getUserProfile(requester.getId()) : null)
                .recipient(recipient != null ? userService.getUserProfile(recipient.getId()) : null)
                .build();
    }
}
