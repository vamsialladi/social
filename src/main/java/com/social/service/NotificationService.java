package com.social.service;

import com.social.entity.Notification;

import java.util.List;

public interface NotificationService {

    Notification sendNotification(String recipientId, String senderId, String type, String message, String referenceId);

    List<Notification> getNotificationsForUser(String recipientId);
}
