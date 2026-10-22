package com.social.service.impl;

import com.social.dto.request.SendMessageRequest;
import com.social.dto.response.MessageResponse;
import com.social.dto.response.MessagesPageResponse;
import com.social.entity.Conversation;
import com.social.entity.Message;
import com.social.event.MessageSentEvent;
import com.social.exception.BadRequestException;
import com.social.repository.ConversationRepository;
import com.social.repository.MessageRepository;
import com.social.service.ConversationService;
import com.social.service.MessageService;
import com.social.service.RateLimiterService;
import com.social.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;
    private final RateLimiterService rateLimiterService;
    private final ApplicationEventPublisher eventPublisher;

    public MessageServiceImpl(MessageRepository messageRepository,
                              ConversationRepository conversationRepository,
                              ConversationService conversationService,
                              RateLimiterService rateLimiterService,
                              ApplicationEventPublisher eventPublisher) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.conversationService = conversationService;
        this.rateLimiterService = rateLimiterService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public MessagesPageResponse getMessages(String conversationId, String userId, String cursor, int limit) {
        conversationService.verifyAndGetConversation(conversationId, userId);

        int pageSize = (limit <= 0) ? 20 : Math.min(limit, 100);
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);

        List<Message> queryResults;
        if (cursor != null && !cursor.isBlank()) {
            Instant cursorInstant = parseCursor(cursor);
            queryResults = messageRepository.findByConversationIdAndCreatedAtLessThanOrderByCreatedAtDesc(
                    conversationId, cursorInstant, pageRequest);
        } else {
            queryResults = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                    conversationId, pageRequest);
        }

        boolean hasMore = queryResults.size() > pageSize;
        List<Message> messagesForPage = hasMore ? queryResults.subList(0, pageSize) : queryResults;

        String nextCursor = null;
        if (hasMore && !messagesForPage.isEmpty()) {
            nextCursor = messagesForPage.get(messagesForPage.size() - 1).getCreatedAt().toString();
        }

        List<MessageResponse> dtos = messagesForPage.stream()
                .map(MessageResponse::fromEntity)
                .collect(Collectors.toList());

        return MessagesPageResponse.builder()
                .messages(dtos)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(String conversationId, String senderId, SendMessageRequest request) {
        rateLimiterService.checkRateLimit(senderId);

        Conversation conversation = conversationService.verifyAndGetConversation(conversationId, senderId);

        if (request == null || !request.isValid()) {
            throw new BadRequestException("Message body is required unless an attachment is present");
        }

        Instant now = Instant.now();

        Message message = Message.builder()
                .id(IdGenerator.nextId())
                .conversationId(conversationId)
                .senderId(senderId)
                .body(request.getBody() != null ? request.getBody().trim() : null)
                .attachments(request.getAttachments() != null ? new ArrayList<>(request.getAttachments()) : new ArrayList<>())
                .readAt(null)
                .createdAt(now)
                .build();

        Message savedMessage = messageRepository.save(message);

        conversation.setLastMessageAt(now);
        conversationRepository.save(conversation);

        List<String> recipients = conversation.getParticipantIds().stream()
                .filter(participantId -> !participantId.equals(senderId))
                .collect(Collectors.toList());

        eventPublisher.publishEvent(MessageSentEvent.builder()
                .conversationId(conversationId)
                .messageId(savedMessage.getId())
                .senderId(senderId)
                .recipientIds(recipients)
                .createdAt(now)
                .build());

        log.info("Message {} sent in conversation {} by user {} to {}",
                savedMessage.getId(), conversationId, senderId, recipients);

        return MessageResponse.fromEntity(savedMessage);
    }

    @Override
    @Transactional
    public int markAsRead(String conversationId, String userId) {
        conversationService.verifyAndGetConversation(conversationId, userId);

        Instant now = Instant.now();
        int updatedCount = messageRepository.markMessagesAsRead(conversationId, userId, now);
        log.info("Marked {} messages as read in conversation {} for user {}", updatedCount, conversationId, userId);
        return updatedCount;
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
