package com.social.service;

import com.social.dto.response.NotificationResponse;
import com.social.dto.response.NotificationsPageResponse;
import com.social.entity.NotificationType;

import java.util.List;

public interface NotificationService {

    NotificationResponse createNotification(String recipientId, String actorId,
                                           NotificationType type, String resourceType, String resourceId);

    NotificationsPageResponse getNotificationsForUser(String userId, String cursor, int limit);

    NotificationResponse markOneAsRead(String notificationId, String userId);

    int markManyAsRead(String userId, List<String> notificationIds);

    long getUnreadCount(String userId);
}
