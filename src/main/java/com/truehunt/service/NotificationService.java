package com.truehunt.service;

import com.truehunt.dto.response.NotificationResponse;
import com.truehunt.dto.response.ProfileResponse;
import com.truehunt.exception.ResourceNotFoundException;
import com.truehunt.model.Notification;
import com.truehunt.model.Profile;
import com.truehunt.repository.NotificationRepository;
import com.truehunt.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private MediaStorageService mediaStorageService;

    @Transactional
    public Notification createNotification(String recipientId, String actorId, String type, String resourceType, String resourceId) {
        if (recipientId == null || recipientId.equals(actorId)) {
            // Do not notify self
            return null;
        }
        Notification notification = new Notification(recipientId, actorId, type, resourceType, resourceId);
        return notificationRepository.save(notification);
    }

    public List<NotificationResponse> getNotifications(String recipientId, String cursor, int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 50);
        PageRequest pageRequest = PageRequest.of(0, pageSize);

        List<Notification> notifications;
        if (cursor != null && !cursor.trim().isEmpty()) {
            Instant cursorTime = Instant.parse(cursor);
            notifications = notificationRepository.findByRecipientIdAndCreatedAtBeforeOrderByCreatedAtDesc(recipientId, cursorTime, pageRequest);
        } else {
            notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageRequest);
        }

        List<String> actorIds = notifications.stream()
                .map(Notification::getActorId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        Map<String, Profile> profileMap = profileRepository.findByUserIdIn(actorIds).stream()
                .collect(Collectors.toMap(Profile::getUserId, p -> p));

        List<NotificationResponse> responses = new ArrayList<>();
        for (Notification n : notifications) {
            NotificationResponse res = new NotificationResponse();
            res.setId(n.getId());
            res.setRecipientId(n.getRecipientId());
            res.setActorId(n.getActorId());
            res.setType(n.getType());
            res.setResourceType(n.getResourceType());
            res.setResourceId(n.getResourceId());
            res.setReadAt(n.getReadAt());
            res.setCreatedAt(n.getCreatedAt());

            if (n.getActorId() != null && profileMap.containsKey(n.getActorId())) {
                Profile actor = profileMap.get(n.getActorId());
                res.setActorName(actor.getName());
                res.setActorProfileImage(mediaStorageService.buildMediaUrl(actor.getProfileImageKey()));
            }
            responses.add(res);
        }
        return responses;
    }

    @Transactional
    public NotificationResponse markOneAsRead(String notificationId, String recipientId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getRecipientId().equals(recipientId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));

        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }

        NotificationResponse res = new NotificationResponse();
        res.setId(notification.getId());
        res.setRecipientId(notification.getRecipientId());
        res.setActorId(notification.getActorId());
        res.setType(notification.getType());
        res.setResourceType(notification.getResourceType());
        res.setResourceId(notification.getResourceId());
        res.setReadAt(notification.getReadAt());
        res.setCreatedAt(notification.getCreatedAt());
        return res;
    }

    @Transactional
    public int markAllAsRead(String recipientId, List<String> notificationIds) {
        Instant now = Instant.now();
        if (notificationIds != null && !notificationIds.isEmpty()) {
            return notificationRepository.markAsReadByIds(notificationIds, recipientId, now);
        } else {
            return notificationRepository.markAllAsReadForUser(recipientId, now);
        }
    }
}
