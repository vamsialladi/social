package com.truehunt.social.service;

import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this(notificationRepository, null);
    }

    @Autowired
    public NotificationService(NotificationRepository notificationRepository, UserAccountRepository userAccountRepository) {
        this.notificationRepository = notificationRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public List<Map<String, Object>> getNotifications() {
        return getNotifications(null);
    }

    public List<Map<String, Object>> getNotifications(String caller) {
        Set<String> callerIdentifiers = resolveCallerIdentifiers(caller);
        return notificationRepository.findAll().stream()
                .filter(n -> matchesCaller(n, callerIdentifiers))
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

    private Long parseLongId(String id) {
        try {
            return Long.parseLong(id);
        } catch (Exception e) {
            return null;
        }
    }
}
