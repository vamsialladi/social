package com.truehunt.social.service;

import com.truehunt.social.dto.ChatMessageDto;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.ConversationEntity;
import com.truehunt.social.model.MessageEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ConversationRepository;
import com.truehunt.social.repository.MessageRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProfileRepository profileRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository,
                          ConversationRepository conversationRepository,
                          UserAccountRepository userAccountRepository,
                          ProfileRepository profileRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userAccountRepository = userAccountRepository;
        this.profileRepository = profileRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ChatMessageDto processAndSendMessage(ChatMessageDto req, Principal principal) {
        if (req == null || req.getContent() == null || req.getContent().trim().isEmpty()) {
            throw new BadRequestException("Message content cannot be empty.");
        }

        // 1. Resolve and authenticate sender
        String senderIdentity = resolveIdentity(principal, req.getSenderId());
        String senderName = req.getSenderName() != null && !req.getSenderName().isBlank()
                ? req.getSenderName()
                : resolveUserName(senderIdentity);

        // 2. Resolve receiver
        String receiverIdentity = req.getReceiverId() != null ? req.getReceiverId().trim() : null;

        // 3. Find or create conversation
        ConversationEntity conversation = null;
        if (req.getConversationId() != null) {
            conversation = conversationRepository.findById(req.getConversationId()).orElse(null);
        }

        if (conversation == null && receiverIdentity != null) {
            conversation = findOrCreateConversation(senderIdentity, senderName, receiverIdentity, req.getReceiverName());
        }

        if (conversation == null) {
            // Fallback: use first available or create general direct conversation
            conversation = new ConversationEntity();
            conversation.setName(receiverIdentity != null ? resolveUserName(receiverIdentity) : "Direct Message");
            conversation.setCategory("Direct");
            conversation.setUser1Id(senderIdentity);
            conversation.setUser1Name(senderName);
            if (receiverIdentity != null) {
                conversation.setUser2Id(receiverIdentity);
                conversation.setUser2Name(resolveUserName(receiverIdentity));
            }
            conversation = conversationRepository.save(conversation);
        }

        Long conversationId = conversation.getId();

        // 4. Update receiver if not yet known
        if (receiverIdentity == null) {
            receiverIdentity = senderIdentity.equalsIgnoreCase(conversation.getUser1Id())
                    ? conversation.getUser2Id()
                    : conversation.getUser1Id();
        }
        String receiverName = resolveUserName(receiverIdentity);

        // 5. Persist message in database
        MessageEntity messageEntity = new MessageEntity(
                conversationId,
                senderIdentity,
                senderName,
                receiverIdentity != null ? receiverIdentity : "unknown",
                receiverName,
                req.getContent().trim(),
                req.getAttachment()
        );
        messageEntity = messageRepository.save(messageEntity);

        // 6. Update conversation entity
        conversation.addMessage(senderIdentity, senderName, receiverIdentity, req.getContent().trim(), "outgoing", req.getAttachment());
        conversation.setPreview(req.getContent().trim());
        conversation.setDate("Just now");
        conversation.setUnread(true);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // 7. Construct DTO for dispatch
        ChatMessageDto outDto = new ChatMessageDto(
                "msg-" + messageEntity.getId(),
                conversationId,
                senderIdentity,
                senderName,
                receiverIdentity,
                receiverName,
                messageEntity.getContent(),
                "outgoing",
                messageEntity.getAttachment(),
                "SENT"
        );
        outDto.setSentAt(LocalDateTime.now().toString());
        outDto.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));
        outDto.setType(req.getType() != null ? req.getType() : "CHAT");

        // 8. Deliver via WebSocket in real-time
        deliverRealtime(outDto, senderIdentity, receiverIdentity, conversationId);

        return outDto;
    }

    private void deliverRealtime(ChatMessageDto outDto, String senderId, String receiverId, Long conversationId) {
        try {
            // A. Broadcast to conversation room topic
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, outDto);
            messagingTemplate.convertAndSend("/topic/messages/" + conversationId, outDto);

            // B. Send to receiver's private queue
            if (receiverId != null && !receiverId.isBlank()) {
                messagingTemplate.convertAndSendToUser(receiverId, "/queue/messages", outDto);

                // Also try alternate identifier (if email, also try publicUserId; if publicUserId, also try email)
                String altReceiverId = resolveAlternateIdentifier(receiverId);
                if (altReceiverId != null && !altReceiverId.equalsIgnoreCase(receiverId)) {
                    messagingTemplate.convertAndSendToUser(altReceiverId, "/queue/messages", outDto);
                }
            }

            // C. Send to sender's private queue for multi-device sync
            if (senderId != null && !senderId.isBlank()) {
                messagingTemplate.convertAndSendToUser(senderId, "/queue/messages", outDto);
            }

            log.info("[WebSocket] Dispatched real-time message from {} to {} in conversation {}",
                    senderId, receiverId, conversationId);
        } catch (Exception ex) {
            log.warn("[WebSocket] Could not deliver real-time message over STOMP: {}", ex.getMessage());
        }
    }

    public List<Map<String, Object>> getMessagesForConversation(Long conversationId, String currentUserId) {
        List<MessageEntity> messages = messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversationId);
        if (!messages.isEmpty()) {
            return messages.stream().map(m -> m.toDto(currentUserId)).collect(Collectors.toList());
        }
        // Fallback to conversation threadJson
        ConversationEntity conv = conversationRepository.findById(conversationId).orElse(null);
        return conv != null ? conv.toDto(currentUserId).get("thread") instanceof List ?
                (List<Map<String, Object>>) conv.toDto(currentUserId).get("thread") : List.of() : List.of();
    }

    private ConversationEntity findOrCreateConversation(String userA, String userAName, String userB, String userBName) {
        // Try user1 = A, user2 = B
        Optional<ConversationEntity> existing = conversationRepository.findByUser1IdAndUser2Id(userA, userB);
        if (existing.isPresent()) return existing.get();

        // Try user1 = B, user2 = A
        existing = conversationRepository.findByUser1IdAndUser2Id(userB, userA);
        if (existing.isPresent()) return existing.get();

        // Try by receiver name
        if (userBName != null && !userBName.isBlank()) {
            Optional<ConversationEntity> byName = conversationRepository.findFirstByNameIgnoreCase(userBName.trim());
            if (byName.isPresent()) {
                ConversationEntity conv = byName.get();
                if (conv.getUser1Id() == null) conv.setUser1Id(userA);
                if (conv.getUser2Id() == null) conv.setUser2Id(userB);
                return conversationRepository.save(conv);
            }
        }

        // Create new
        ConversationEntity conv = new ConversationEntity();
        conv.setUser1Id(userA);
        conv.setUser1Name(userAName);
        conv.setUser2Id(userB);
        conv.setUser2Name(userBName != null ? userBName : resolveUserName(userB));
        conv.setName(userBName != null ? userBName : resolveUserName(userB));
        conv.setCategory("Direct");
        conv.setMailbox("Focused");
        conv.setPreview("Start of conversation");
        conv.setDate("Just now");
        return conversationRepository.save(conv);
    }

    private String resolveIdentity(Principal principal, String fallback) {
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName().trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return "pro@truehunt.in";
    }

    private String resolveUserName(String identifier) {
        if (identifier == null || identifier.isBlank()) return "Cinema Professional";
        Optional<UserAccountEntity> account = userAccountRepository.findByEmail(identifier.toLowerCase(Locale.ROOT));
        if (account.isPresent()) return account.get().getName();

        Optional<ProfileEntity> profile = profileRepository.findByUserId(identifier);
        if (profile.isPresent()) return profile.get().getName();

        return identifier;
    }

    private String resolveAlternateIdentifier(String identifier) {
        if (identifier == null) return null;
        if (identifier.contains("@")) {
            return userAccountRepository.findByEmail(identifier.toLowerCase(Locale.ROOT))
                    .map(UserAccountEntity::getPublicUserId)
                    .orElse(null);
        } else {
            return userAccountRepository.findByPublicUserId(identifier)
                    .map(UserAccountEntity::getEmail)
                    .orElse(null);
        }
    }
}

