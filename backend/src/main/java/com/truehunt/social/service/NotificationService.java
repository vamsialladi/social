package com.truehunt.social.service;

import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final SettingsService settingsService;
    private final ConnectionService connectionService;

    public NotificationService(NotificationRepository notificationRepository) {
        this(notificationRepository, null, null, null);
    }

    public NotificationService(NotificationRepository notificationRepository, UserAccountRepository userAccountRepository) {
        this(notificationRepository, userAccountRepository, null, null);
    }

    public NotificationService(
            NotificationRepository notificationRepository,
            UserAccountRepository userAccountRepository,
            SettingsService settingsService
    ) {
        this(notificationRepository, userAccountRepository, settingsService, null);
    }

    @Autowired
    public NotificationService(
            NotificationRepository notificationRepository,
            UserAccountRepository userAccountRepository,
            @Autowired(required = false) SettingsService settingsService,
            ConnectionService connectionService
    ) {
        this.notificationRepository = notificationRepository;
        this.userAccountRepository = userAccountRepository;
        this.settingsService = settingsService;
        this.connectionService = connectionService;
    }

    public List<Map<String, Object>> getNotifications() {
        return getNotifications(null);
    }

    public List<Map<String, Object>> getNotifications(String caller) {
        Set<String> callerIdentifiers = resolveCallerIdentifiers(caller);
        boolean connectionNotifsEnabled = settingsService == null
                || (settingsService.isConnectingWithOthersEnabled(caller)
                    && (callerIdentifiers.isEmpty() || callerIdentifiers.stream().allMatch(settingsService::isConnectingWithOthersEnabled)));
        boolean postingNotifsEnabled = settingsService == null
                || (settingsService.isPostingAndCommentingEnabled(caller)
                    && (callerIdentifiers.isEmpty() || callerIdentifiers.stream().allMatch(settingsService::isPostingAndCommentingEnabled)));
        boolean profileNotifsEnabled = settingsService == null
                || (settingsService.isProfileUpdatingEnabled(caller)
                    && (callerIdentifiers.isEmpty() || callerIdentifiers.stream().allMatch(settingsService::isProfileUpdatingEnabled)));
        return notificationRepository.findAll().stream()
                .filter(n -> matchesCaller(n, callerIdentifiers))
                .filter(n -> connectionNotifsEnabled || n.getType() == null || !n.getType().toLowerCase(Locale.ROOT).startsWith("connection"))
                .filter(n -> postingNotifsEnabled || n.getType() == null || (
                        !n.getType().toLowerCase(Locale.ROOT).startsWith("post")
                                && !n.getType().toLowerCase(Locale.ROOT).startsWith("opportunity_post")
                                && !n.getType().toLowerCase(Locale.ROOT).startsWith("comment")))
                .filter(n -> profileNotifsEnabled || n.getType() == null || !n.getType().toLowerCase(Locale.ROOT).startsWith("profile"))
                .sorted(Comparator.comparing(NotificationEntity::getCreatedAt).reversed())
                .map(NotificationEntity::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> markAsRead(String id) {
        Long numericId = parseLongId(id);
        NotificationEntity n = (numericId != null ? notificationRepository.findById(numericId) : Optional.<NotificationEntity>empty())
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        n.setRead(true);
        notificationRepository.save(n);
        return n.toDto();
    }

    @Transactional
    public Map<String, Object> markAllAsRead() {
        return markAllAsRead(null);
    }

    @Transactional
    public Map<String, Object> markAllAsRead(String caller) {
        Set<String> callerIdentifiers = resolveCallerIdentifiers(caller);
        List<NotificationEntity> list = notificationRepository.findAll().stream()
                .filter(n -> matchesCaller(n, callerIdentifiers))
                .collect(Collectors.toList());
        for (NotificationEntity n : list) {
            n.setRead(true);
        }
        notificationRepository.saveAll(list);
        Map<String, Object> res = new HashMap<>();
        res.put("message", "All notifications marked as read");
        return res;
    }

    private Set<String> resolveCallerIdentifiers(String caller) {
        if (caller == null || caller.isBlank()) {
            return Collections.emptySet();
        }
        String trimmed = caller.trim();
        Set<String> identifiers = new HashSet<>();
        identifiers.add(trimmed.toLowerCase(Locale.ROOT));
        if (userAccountRepository != null) {
            Optional<UserAccountEntity> account = userAccountRepository.findByEmail(trimmed.toLowerCase(Locale.ROOT))
                    .or(() -> userAccountRepository.findByPublicUserId(trimmed));
            account.ifPresent(user -> {
                if (user.getEmail() != null && !user.getEmail().isBlank()) {
                    identifiers.add(user.getEmail().trim().toLowerCase(Locale.ROOT));
                }
                if (user.getPublicUserId() != null && !user.getPublicUserId().isBlank()) {
                    identifiers.add(user.getPublicUserId().trim().toLowerCase(Locale.ROOT));
                }
                if (user.getId() != null) {
                    identifiers.add(String.valueOf(user.getId()));
                }
            });
        }
        return identifiers;
    }

    private boolean matchesCaller(NotificationEntity notification, Set<String> callerIdentifiers) {
        if (callerIdentifiers.isEmpty()) {
            return true;
        }
        String target = notification.getRecipientId();
        if (target == null || target.isBlank() || "all".equalsIgnoreCase(target.trim())) {
            return true;
        }
        return callerIdentifiers.contains(target.trim().toLowerCase(Locale.ROOT));
    }

    @Transactional
    public void deleteNotification(String id) {
        Long numericId = parseLongId(id);
        if (numericId == null || !notificationRepository.existsById(numericId)) {
            throw new ResourceNotFoundException("Notification", "id", id);
        }
        notificationRepository.deleteById(numericId);
    }

    @Transactional
    public NotificationEntity createNotification(String userId, String title, String message, String type) {
        NotificationEntity n = new NotificationEntity(userId, title, message, type);
        return notificationRepository.save(n);
    }

    @Transactional
    public List<Map<String, Object>> sendEndorsementRequests(
            String caller,
            String senderProfileId,
            List<String> requestedRecipientIds
    ) {
        if (connectionService == null) {
            throw new IllegalStateException("Connection verification is unavailable.");
        }
        if (requestedRecipientIds == null || requestedRecipientIds.isEmpty()) {
            throw new BadRequestException("Select at least one connection.");
        }
        if (requestedRecipientIds.size() > 100) {
            throw new BadRequestException("You can select up to 100 connections at a time.");
        }

        validateSenderProfileId(caller, senderProfileId);
        LocalDateTime now = LocalDateTime.now();
        List<NotificationEntity> existingNotifications = notificationRepository.findAll();

        Map<String, Map<String, Object>> connectedByIdentity = new LinkedHashMap<>();
        for (Map<String, Object> profile : connectionService.getConnectedProfiles(caller)) {
            for (String identifier : profileIdentifiers(profile)) {
                connectedByIdentity.putIfAbsent(identifier.toLowerCase(Locale.ROOT), profile);
            }
        }

        Map<String, NotificationEntity> notificationsByRecipient = new LinkedHashMap<>();
        for (String requestedId : requestedRecipientIds) {
            if (requestedId == null || requestedId.isBlank()) {
                throw new BadRequestException("A selected connection is missing a profile ID.");
            }
            String recipientKey = requestedId.trim().toLowerCase(Locale.ROOT);
            Map<String, Object> connectedProfile = connectedByIdentity.get(recipientKey);
            if (connectedProfile == null) {
                Set<String> aliases = normalizeIdentifiers(connectionService.resolveAllUserIdentifiers(requestedId));
                connectedProfile = aliases.stream()
                        .map(connectedByIdentity::get)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
            }
            if (connectedProfile == null) {
                throw new BadRequestException("You can only send endorsement requests to your connections.");
            }

            String recipientId = firstProfileIdentifier(connectedProfile);
            if (recipientId == null) {
                throw new BadRequestException("A selected connection has no valid profile ID.");
            }
            String canonicalRecipientKey = recipientId.toLowerCase(Locale.ROOT);
            if (!notificationsByRecipient.containsKey(canonicalRecipientKey)) {
                if (hasRecentEndorsementRequest(
                        existingNotifications,
                        senderProfileId,
                        recipientId,
                        now
                )) {
                    throw new BadRequestException(
                            "You can send another endorsement request to this connection after 24 hours."
                    );
                }
                notificationsByRecipient.put(
                        canonicalRecipientKey,
                        createEndorsementRequestNotification(recipientId, senderProfileId.trim(), now)
                );
            }
        }

        return notificationsByRecipient.values().stream()
                .map(NotificationEntity::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEndorsementRequestCooldowns(String caller, String senderProfileId) {
        validateSenderProfileId(caller, senderProfileId);
        LocalDateTime now = LocalDateTime.now();
        Map<String, Long> remainingByRecipient = new LinkedHashMap<>();
        for (NotificationEntity notification : notificationRepository.findAll()) {
            if (!isEndorsementRequestFrom(notification, senderProfileId)) {
                continue;
            }
            LocalDateTime createdAt = notification.getCreatedAt();
            if (createdAt == null) {
                continue;
            }
            long remainingSeconds = Duration.between(now, createdAt.plusHours(24)).getSeconds();
            if (remainingSeconds > 0) {
                String recipientId = notification.getRecipientId();
                if (recipientId != null && !recipientId.isBlank()) {
                    remainingByRecipient.merge(
                            recipientId.trim(),
                            remainingSeconds,
                            Math::max
                    );
                }
            }
        }

        return remainingByRecipient.entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "recipientProfileId", entry.getKey(),
                        "remainingSeconds", entry.getValue()
                ))
                .toList();
    }

    private void validateSenderProfileId(String caller, String senderProfileId) {
        Set<String> senderIdentifiers = normalizeIdentifiers(connectionService.resolveAllUserIdentifiers(caller));
        if (senderProfileId == null || !senderIdentifiers.contains(senderProfileId.trim().toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("The sender profile does not match your signed-in account.");
        }
    }

    private boolean hasRecentEndorsementRequest(
            List<NotificationEntity> notifications,
            String senderProfileId,
            String recipientId,
            LocalDateTime now
    ) {
        return notifications.stream().anyMatch(notification ->
                isEndorsementRequestFrom(notification, senderProfileId)
                        && notification.getRecipientId() != null
                        && notification.getRecipientId().equalsIgnoreCase(recipientId)
                        && notification.getCreatedAt() != null
                        && notification.getCreatedAt().plusHours(24).isAfter(now)
        );
    }

    private boolean isEndorsementRequestFrom(NotificationEntity notification, String senderProfileId) {
        return "endorsement_request".equalsIgnoreCase(notification.getType())
                && notification.getSenderProfileId() != null
                && notification.getSenderProfileId().equalsIgnoreCase(senderProfileId);
    }

    private NotificationEntity createEndorsementRequestNotification(
            String recipientId,
            String senderProfileId,
            LocalDateTime createdAt
    ) {
        NotificationEntity notification = new NotificationEntity(
                recipientId,
                "Endorsement request",
                "A connection has asked you to endorse their profile.",
                "endorsement_request"
        );
        notification.setSenderProfileId(senderProfileId);
        notification.setCreatedAt(createdAt);
        return notificationRepository.save(notification);
    }

    private Set<String> normalizeIdentifiers(Set<String> identifiers) {
        if (identifiers == null) {
            return Collections.emptySet();
        }
        return identifiers.stream()
                .filter(identifier -> identifier != null && !identifier.isBlank())
                .map(identifier -> identifier.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private List<String> profileIdentifiers(Map<String, Object> profile) {
        return List.of("publicUserId", "userId", "profileId", "id", "email").stream()
                .map(profile::get)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(identifier -> !identifier.isBlank())
                .toList();
    }

    private String firstProfileIdentifier(Map<String, Object> profile) {
        return profileIdentifiers(profile).stream().findFirst().orElse(null);
    }

    private Long parseLongId(String id) {
        if (id == null) return null;
        String clean = id.trim();
        if (clean.toLowerCase(Locale.ROOT).startsWith("notif-")) {
            clean = clean.substring("notif-".length());
        }
        try {
            return Long.parseLong(clean);
        } catch (Exception e) {
            return null;
        }
    }
}
