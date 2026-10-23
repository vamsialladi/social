package com.truehunt.service;

import com.truehunt.dto.request.SendMessageRequest;
import com.truehunt.dto.response.ConversationResponse;
import com.truehunt.dto.response.MessageResponse;
import com.truehunt.dto.response.ProfileResponse;
import com.truehunt.exception.ForbiddenException;
import com.truehunt.exception.ResourceNotFoundException;
import com.truehunt.exception.ValidationException;
import com.truehunt.model.Conversation;
import com.truehunt.model.Message;
import com.truehunt.model.Profile;
import com.truehunt.repository.ConversationRepository;
import com.truehunt.repository.MessageRepository;
import com.truehunt.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private NotificationService notificationService;

    public List<ConversationResponse> listConversations(String currentUserId) {
        List<Conversation> conversations = conversationRepository.findAllByOrderByLastMessageAtDesc().stream()
                .filter(conv -> conv.getParticipantIds() != null && conv.getParticipantIds().contains(currentUserId))
                .collect(Collectors.toList());
        List<ConversationResponse> responses = new ArrayList<>();

        for (Conversation conv : conversations) {
            ConversationResponse res = new ConversationResponse();
            res.setId(conv.getId());
            res.setParticipantIds(conv.getParticipantIds());
            res.setLastMessageAt(conv.getLastMessageAt());
            res.setCreatedAt(conv.getCreatedAt());

            // Load participant profiles
            List<Profile> profiles = profileRepository.findByUserIdIn(conv.getParticipantIds());
            List<ProfileResponse> participantResponses = profiles.stream()
                    .map(p -> profileService.toResponse(p, p.getUserId().equals(currentUserId), true))
                    .collect(Collectors.toList());
            res.setParticipants(participantResponses);

            // Fetch latest message
            List<Message> latestMessages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conv.getId(), PageRequest.of(0, 1));
            if (!latestMessages.isEmpty()) {
                res.setLastMessage(toMessageResponse(latestMessages.get(0)));
            }

            responses.add(res);
        }

        return responses;
    }

    @Transactional
    public List<MessageResponse> getMessages(String conversationId, String currentUserId, String cursor, int limit) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));

        if (!conversation.getParticipantIds().contains(currentUserId)) {
            throw new ForbiddenException("You are not a participant in this conversation.");
        }

        int pageSize = Math.min(Math.max(limit, 1), 50);
        PageRequest pageRequest = PageRequest.of(0, pageSize);

        List<Message> messages;
        if (cursor != null && !cursor.trim().isEmpty()) {
            Instant cursorTime = Instant.parse(cursor);
            messages = messageRepository.findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(conversationId, cursorTime, pageRequest);
        } else {
            messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageRequest);
        }

        // Mark unread messages sent by others as read
        Instant now = Instant.now();
        for (Message msg : messages) {
            if (!msg.getSenderId().equals(currentUserId) && msg.getReadAt() == null) {
                msg.setReadAt(now);
                messageRepository.save(msg);
            }
        }

        return messages.stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse sendMessage(String conversationId, String currentUserId, SendMessageRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));

        if (!conversation.getParticipantIds().contains(currentUserId)) {
            throw new ForbiddenException("You are not a participant in this conversation.");
        }

        if (request.getBody() == null || request.getBody().isBlank()) {
            throw new ValidationException("Message body cannot be empty.");
        }

        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(currentUserId);
        message.setBody(request.getBody().trim());
        message.setAttachments(request.getAttachments() != null ? request.getAttachments() : new ArrayList<>());

        message = messageRepository.save(message);

        // Update conversation lastMessageAt
        conversation.setLastMessageAt(message.getCreatedAt());
        conversationRepository.save(conversation);

        // Notify other participants
        for (String participantId : conversation.getParticipantIds()) {
            if (!participantId.equals(currentUserId)) {
                notificationService.createNotification(
                        participantId,
                        currentUserId,
                        "message",
                        "conversation",
                        conversationId
                );
            }
        }

        return toMessageResponse(message);
    }

    @Transactional
    public ConversationResponse getOrCreateDirectConversation(String currentUserId, String otherUserId) {
        if (currentUserId.equals(otherUserId)) {
            throw new ValidationException("Cannot start conversation with yourself.");
        }

        List<Conversation> userConversations = conversationRepository.findAllByOrderByLastMessageAtDesc().stream()
                .filter(conv -> conv.getParticipantIds() != null && conv.getParticipantIds().contains(currentUserId))
                .collect(Collectors.toList());
        for (Conversation conv : userConversations) {
            if (conv.getParticipantIds().size() == 2 && conv.getParticipantIds().contains(otherUserId)) {
                return toConversationResponse(conv, currentUserId);
            }
        }

        Conversation newConv = new Conversation();
        newConv.setParticipantIds(Arrays.asList(currentUserId, otherUserId));
        newConv = conversationRepository.save(newConv);

        return toConversationResponse(newConv, currentUserId);
    }

    private ConversationResponse toConversationResponse(Conversation conv, String currentUserId) {
        ConversationResponse res = new ConversationResponse();
        res.setId(conv.getId());
        res.setParticipantIds(conv.getParticipantIds());
        res.setLastMessageAt(conv.getLastMessageAt());
        res.setCreatedAt(conv.getCreatedAt());

        List<Profile> profiles = profileRepository.findByUserIdIn(conv.getParticipantIds());
        res.setParticipants(profiles.stream()
                .map(p -> profileService.toResponse(p, p.getUserId().equals(currentUserId), true))
                .collect(Collectors.toList()));
        return res;
    }

    public MessageResponse toMessageResponse(Message msg) {
        MessageResponse res = new MessageResponse();
        res.setId(msg.getId());
        res.setConversationId(msg.getConversationId());
        res.setSenderId(msg.getSenderId());
        res.setBody(msg.getBody());
        res.setAttachments(msg.getAttachments());
        res.setReadAt(msg.getReadAt());
        res.setCreatedAt(msg.getCreatedAt());

        profileRepository.findByUserId(msg.getSenderId()).ifPresent(profile -> {
            res.setSenderName(profile.getName());
        });

        return res;
    }
}
