package com.social.service;

import com.social.dto.response.NotificationResponse;
import com.social.dto.response.NotificationsPageResponse;
import com.social.entity.Notification;
import com.social.entity.NotificationType;

import java.util.List;

public interface NotificationService {

    NotificationResponse createNotification(String recipientId, String actorId,
                                           NotificationType type, String resourceType, String resourceId);

    Notification sendNotification(String recipientId, String senderId, String type, String message, String referenceId);

    NotificationsPageResponse getNotificationsForUser(String userId, String cursor, int limit);

    List<Notification> getNotificationsForUser(String recipientId);

    NotificationResponse markOneAsRead(String notificationId, String userId);

    int markManyAsRead(String userId, List<String> notificationIds);

    long getUnreadCount(String userId);
}
