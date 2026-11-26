package com.truehunt.social.service;

import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.EndorsementRequestEntity;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.EndorsementRequestRepository;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final long ENDORSEMENT_REQUEST_COOLDOWN_SECONDS = 86_400L; // 24 hours

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final SettingsService settingsService;
    private final EndorsementRequestRepository endorsementRequestRepository;
    private final ProfileRepository profileRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this(notificationRepository, null, null, null, null);
    }

    public NotificationService(NotificationRepository notificationRepository, UserAccountRepository userAccountRepository) {
        this(notificationRepository, userAccountRepository, null, null, null);
    }

    @Autowired
    public NotificationService(
            NotificationRepository notificationRepository,
            @Autowired(required = false) UserAccountRepository userAccountRepository,
            @Autowired(required = false) SettingsService settingsService,
            @Autowired(required = false) EndorsementRequestRepository endorsementRequestRepository,
            @Autowired(required = false) ProfileRepository profileRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userAccountRepository = userAccountRepository;
        this.settingsService = settingsService;
        this.endorsementRequestRepository = endorsementRequestRepository;
        this.profileRepository = profileRepository;
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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEndorsementRequestCooldowns(String senderProfileId) {
        if (senderProfileId == null || senderProfileId.isBlank() || endorsementRequestRepository == null) {
            return Collections.emptyList();
        }
        String cleanSenderId = senderProfileId.trim();
        LocalDateTime now = LocalDateTime.now();
        List<EndorsementRequestEntity> requests = endorsementRequestRepository.findAllBySenderProfileIdIgnoreCase(cleanSenderId);
        List<Map<String, Object>> cooldowns = new ArrayList<>();
        for (EndorsementRequestEntity req : requests) {
            long elapsedSeconds = Duration.between(req.getRequestedAt(), now).getSeconds();
            long remainingSeconds = Math.max(0, ENDORSEMENT_REQUEST_COOLDOWN_SECONDS - elapsedSeconds);
            if (remainingSeconds > 0) {
                Map<String, Object> map = new HashMap<>();
                map.put("recipientProfileId", req.getRecipientProfileId());
                map.put("remainingSeconds", remainingSeconds);
                cooldowns.add(map);
            }
        }
        return cooldowns;
    }

    @Transactional
    public List<Map<String, Object>> sendEndorsementRequests(
            String senderProfileId,
            List<String> recipientProfileIds,
            String callerIdentifier
    ) {
        if (senderProfileId == null || senderProfileId.isBlank()) {
            throw new BadRequestException("Sender profile ID is required.");
        }
        if (recipientProfileIds == null || recipientProfileIds.isEmpty()) {
            return Collections.emptyList();
        }
        String cleanSenderId = senderProfileId.trim();
        String senderName = "A connected colleague";
        if (profileRepository != null) {
            Optional<ProfileEntity> senderProfile = profileRepository.findByUserId(cleanSenderId)
                    .or(() -> profileRepository.findByEmail(cleanSenderId));
            if (senderProfile.isPresent() && senderProfile.get().getName() != null && !senderProfile.get().getName().isBlank()) {
                senderName = senderProfile.get().getName().trim();
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> createdNotifications = new ArrayList<>();

        for (String recipientId : recipientProfileIds) {
            if (recipientId == null || recipientId.isBlank()) continue;
            String cleanRecipientId = recipientId.trim();
            if (cleanRecipientId.equalsIgnoreCase(cleanSenderId)) continue;

            if (endorsementRequestRepository != null) {
                Optional<EndorsementRequestEntity> existing = endorsementRequestRepository
                        .findBySenderProfileIdIgnoreCaseAndRecipientProfileIdIgnoreCase(cleanSenderId, cleanRecipientId);
                if (existing.isPresent()) {
                    long elapsed = Duration.between(existing.get().getRequestedAt(), now).getSeconds();
                    if (elapsed < ENDORSEMENT_REQUEST_COOLDOWN_SECONDS) {
                        continue; // Cooldown active for this recipient
                    }
                    existing.get().setRequestedAt(now);
                    endorsementRequestRepository.save(existing.get());
                } else {
                    endorsementRequestRepository.save(new EndorsementRequestEntity(cleanSenderId, cleanRecipientId, now));
                }
            }

            NotificationEntity notif = new NotificationEntity();
            notif.setRecipientId(cleanRecipientId);
            notif.setSenderProfileId(cleanSenderId);
            notif.setType("endorsement_request");
            notif.setTitle("Endorsement Request");
            notif.setMessage(senderName + " requested an endorsement from you.");
            notif.setRead(false);
            notif.setCreatedAt(now);
            NotificationEntity saved = notificationRepository.save(notif);
            createdNotifications.add(saved.toDto());
        }

        return createdNotifications;
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
