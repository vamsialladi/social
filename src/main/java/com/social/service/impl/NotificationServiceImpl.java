package com.social.service.impl;

import com.social.dto.response.NotificationResponse;
import com.social.dto.response.NotificationsPageResponse;
import com.social.entity.Notification;
import com.social.entity.NotificationType;
import com.social.exception.BadRequestException;
import com.social.exception.ForbiddenException;
import com.social.exception.ResourceNotFoundException;
import com.social.repository.NotificationRepository;
import com.social.service.NotificationService;
import com.social.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public NotificationResponse createNotification(String recipientId, String actorId,
                                                   NotificationType type, String resourceType, String resourceId) {
        if (recipientId == null || recipientId.isBlank()) {
            throw new IllegalArgumentException("recipientId cannot be null or empty");
        }
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("actorId cannot be null or empty");
        }

        // Avoid self-notification
        if (recipientId.equals(actorId)) {
            log.debug("Skipping self-notification for user {}", actorId);
            return null;
        }

        Notification notification = Notification.builder()
                .id(IdGenerator.nextId())
                .recipientId(recipientId)
                .actorId(actorId)
                .type(type)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .readAt(null)
                .createdAt(Instant.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification {} for recipient {} from actor {} of type {}",
                saved.getId(), recipientId, actorId, type);

        return NotificationResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationsPageResponse getNotificationsForUser(String userId, String cursor, int limit) {
        int pageSize = (limit <= 0) ? 20 : Math.min(limit, 100);
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);

        List<Notification> queryResults;
        if (cursor != null && !cursor.isBlank()) {
            Instant cursorInstant = parseCursor(cursor);
            queryResults = notificationRepository.findByRecipientIdAndCreatedAtLessThanOrderByCreatedAtDesc(
                    userId, cursorInstant, pageRequest);
        } else {
            queryResults = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageRequest);
        }

        boolean hasMore = queryResults.size() > pageSize;
        List<Notification> pagedList = hasMore ? queryResults.subList(0, pageSize) : queryResults;

        String nextCursor = null;
        if (hasMore && !pagedList.isEmpty()) {
            nextCursor = pagedList.get(pagedList.size() - 1).getCreatedAt().toString();
        }

        long unreadCount = notificationRepository.countByRecipientIdAndReadAtIsNull(userId);

        List<NotificationResponse> dtos = pagedList.stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());

        return NotificationsPageResponse.builder()
                .notifications(dtos)
                .unreadCount(unreadCount)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    @Override
    @Transactional
    public NotificationResponse markOneAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getRecipientId().equals(userId)) {
            log.warn("Unauthorized attempt to mark notification {} read by user {}", notificationId, userId);
            throw new ForbiddenException("You do not have permission to modify this notification");
        }

        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
            log.info("Marked notification {} as read for user {}", notificationId, userId);
        }

        return NotificationResponse.fromEntity(notification);
    }

    @Override
    @Transactional
    public int markManyAsRead(String userId, List<String> notificationIds) {
        Instant now = Instant.now();
        int updatedCount;

        if (notificationIds != null && !notificationIds.isEmpty()) {
            updatedCount = notificationRepository.markBatchAsRead(userId, notificationIds, now);
            log.info("Marked {} specific notifications as read for user {}", updatedCount, userId);
        } else {
            updatedCount = notificationRepository.markAllAsRead(userId, now);
            log.info("Marked all ({}) unread notifications as read for user {}", updatedCount, userId);
        }

        return updatedCount;
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(userId);
    }

    private Instant parseCursor(String cursor) {
        try {
            return Instant.parse(cursor.trim());
        } catch (DateTimeParseException e) {
            try {
                long epochMillis = Long.parseLong(cursor.trim());
                return Instant.ofEpochMilli(epochMillis);
            } catch (NumberFormatException nfe) {
                throw new BadRequestException("Invalid cursor format. Expected ISO-8601 timestamp or epoch milliseconds.");
            }
        }
    }
}
