package com.social.event;

import com.social.entity.NotificationType;
import com.social.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async
    @EventListener
    public void onMessageSent(MessageSentEvent event) {
        log.info("Processing async notification for message {}: sending to recipients {}", 
                event.getMessageId(), event.getRecipientIds());

        for (String recipientId : event.getRecipientIds()) {
            try {
                // Rule: Never expose private message content; store only reference metadata
                notificationService.createNotification(
                        recipientId,
                        event.getSenderId(),
                        NotificationType.MESSAGE,
                        "conversation",
                        event.getConversationId()
                );
            } catch (Exception e) {
                log.error("Failed to create message notification for recipient {}", recipientId, e);
            }
        }
    }
}
