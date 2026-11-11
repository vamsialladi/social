package com.truehunt.social.service;

import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Map<String, Object>> getNotifications() {
        return notificationRepository.findAll().stream()
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
        List<NotificationEntity> list = notificationRepository.findAll();
        for (NotificationEntity n : list) {
            n.setRead(true);
        }
        notificationRepository.saveAll(list);
        Map<String, Object> res = new HashMap<>();
        res.put("message", "All notifications marked as read");
        return res;
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
