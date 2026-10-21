package com.social.service.impl;

import com.social.entity.Notification;
import com.social.repository.NotificationRepository;
import com.social.service.NotificationService;
import com.social.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public Notification sendNotification(String recipientId, String senderId, String type, String message, String referenceId) {
        log.info("Creating notification of type {} for recipient {}", type, recipientId);
        Notification notification = Notification.builder()
                .id(IdGenerator.nextObjectId())
                .recipientId(recipientId)
                .senderId(senderId)
                .type(type)
                .message(message)
                .referenceId(referenceId)
                .read(false)
                .createdAt(Instant.now())
                .build();
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(String recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }
}
