package com.social.service.impl;

import com.social.dto.request.CreateConversationRequest;
import com.social.dto.response.ConversationResponse;
import com.social.dto.response.ConversationsPageResponse;
import com.social.dto.response.MessageResponse;
import com.social.entity.Conversation;
import com.social.entity.Message;
import com.social.exception.ForbiddenException;
import com.social.exception.ResourceNotFoundException;
import com.social.repository.ConversationRepository;
import com.social.repository.MessageRepository;
import com.social.service.ConnectionCheckService;
import com.social.service.ConversationService;
import com.social.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationServiceImpl implements ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationServiceImpl.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConnectionCheckService connectionCheckService;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   MessageRepository messageRepository,
                                   ConnectionCheckService connectionCheckService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.connectionCheckService = connectionCheckService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversationsForUser(String userId) {
        return getConversationsForUser(userId, null, 50).getConversations();
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationsPageResponse getConversationsForUser(String userId, String cursor, int limit) {
        int pageSize = (limit <= 0) ? 20 : Math.min(limit, 100);
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(0, pageSize + 1);

        List<Conversation> queryResults;
        if (cursor != null && !cursor.isBlank()) {
            Instant cursorInstant = parseCursor(cursor);
            queryResults = conversationRepository.findConversationsByParticipantIdBefore(userId, cursorInstant, pageRequest);
        } else {
            queryResults = conversationRepository.findConversationsByParticipantId(userId, pageRequest);
        }

        boolean hasMore = queryResults.size() > pageSize;
        List<Conversation> pagedList = hasMore ? queryResults.subList(0, pageSize) : queryResults;

        String nextCursor = null;
        if (hasMore && !pagedList.isEmpty()) {
            Conversation last = pagedList.get(pagedList.size() - 1);
            Instant cursorTime = last.getLastMessageAt() != null ? last.getLastMessageAt() : last.getCreatedAt();
            nextCursor = cursorTime != null ? cursorTime.toString() : null;
        }

        List<ConversationResponse> dtos = pagedList.stream().map(conv -> {
            ConversationResponse response = ConversationResponse.fromEntity(conv);
            long unreadCount = messageRepository.countByConversationIdAndSenderIdNotAndReadAtIsNull(conv.getId(), userId);
            response.setUnreadCount(unreadCount);
            Optional<Message> latestMessage = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conv.getId());
            latestMessage.ifPresent(msg -> response.setLastMessage(MessageResponse.fromEntity(msg)));
            return response;
        }).collect(Collectors.toList());

        return new ConversationsPageResponse(dtos, nextCursor, hasMore);
    }

    private Instant parseCursor(String cursor) {
        try {
            return Instant.parse(cursor.trim());
        } catch (java.time.format.DateTimeParseException e) {
            try {
                long epochMillis = Long.parseLong(cursor.trim());
                return Instant.ofEpochMilli(epochMillis);
            } catch (NumberFormatException nfe) {
                throw new com.social.exception.BadRequestException("Invalid cursor format. Expected ISO-8601 timestamp or epoch milliseconds.");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversationById(String conversationId, String userId) {
        Conversation conversation = verifyAndGetConversation(conversationId, userId);
        ConversationResponse response = ConversationResponse.fromEntity(conversation);

        long unreadCount = messageRepository.countByConversationIdAndSenderIdNotAndReadAtIsNull(conversation.getId(), userId);
        response.setUnreadCount(unreadCount);

        Optional<Message> latestMessage = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId());
        latestMessage.ifPresent(msg -> response.setLastMessage(MessageResponse.fromEntity(msg)));

        return response;
    }

    @Override
    @Transactional
    public ConversationResponse createOrGetConversation(String userId, CreateConversationRequest request) {
        Set<String> participants = new LinkedHashSet<>();
        participants.add(userId);

        if (request.getParticipantId() != null && !request.getParticipantId().isBlank()) {
            participants.add(request.getParticipantId().trim());
        }
        if (request.getParticipantIds() != null) {
            for (String pid : request.getParticipantIds()) {
                if (pid != null && !pid.isBlank()) {
                    participants.add(pid.trim());
                }
            }
        }

        if (participants.size() < 2) {
            throw new IllegalArgumentException("A conversation requires at least two participants");
        }

        List<String> participantList = new ArrayList<>(participants);

        if (participantList.size() == 2) {
            String otherUser = participantList.get(0).equals(userId) ? participantList.get(1) : participantList.get(0);
            connectionCheckService.validateCanStartConversation(userId, otherUser);

            Optional<Conversation> existing = conversationRepository.findDirectConversation(participantList.get(0), participantList.get(1));
            if (existing.isPresent()) {
                log.info("Found existing conversation {} between {} and {}", existing.get().getId(), participantList.get(0), participantList.get(1));
                return getConversationById(existing.get().getId(), userId);
            }
        }

        Instant now = Instant.now();
        Conversation newConv = Conversation.builder()
                .id(IdGenerator.nextId())
                .participantIds(participantList)
                .createdAt(now)
                .lastMessageAt(now)
                .build();

        Conversation saved = conversationRepository.save(newConv);
        log.info("Created new conversation {} with participants {}", saved.getId(), saved.getParticipantIds());

        ConversationResponse response = ConversationResponse.fromEntity(saved);
        response.setUnreadCount(0);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Conversation verifyAndGetConversation(String conversationId, String userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        if (!conversation.getParticipantIds().contains(userId)) {
            log.warn("Access denied: user {} is not a participant of conversation {}", userId, conversationId);
            throw new ForbiddenException("You are not a participant of this conversation");
        }

        return conversation;
    }
}
