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
        String senderRawId = resolveIdentity(principal, req.getSenderId());
        Optional<UserAccountEntity> senderOpt = resolveUserAccount(senderRawId);
        String senderEmail = senderOpt.map(UserAccountEntity::getEmail).orElse(senderRawId);
        String senderPublicUserId = senderOpt.map(UserAccountEntity::getPublicUserId).orElse(senderRawId);
        String senderName = req.getSenderName() != null && !req.getSenderName().isBlank()
                ? req.getSenderName()
                : senderOpt.map(UserAccountEntity::getName).orElse(resolveUserName(senderRawId));

        // 2. Resolve receiver
        String receiverRawId = req.getReceiverId() != null ? req.getReceiverId().trim() : null;
        Optional<UserAccountEntity> receiverOpt = resolveUserAccount(receiverRawId);
        if (receiverOpt.isEmpty() && req.getReceiverName() != null) {
            receiverOpt = resolveUserAccount(req.getReceiverName());
        }
        String receiverEmail = receiverOpt.map(UserAccountEntity::getEmail).orElse(receiverRawId);
        String receiverPublicUserId = receiverOpt.map(UserAccountEntity::getPublicUserId).orElse(receiverRawId);
        String receiverName = req.getReceiverName() != null && !req.getReceiverName().isBlank()
                ? req.getReceiverName()
                : receiverOpt.map(UserAccountEntity::getName).orElse(resolveUserName(receiverRawId));

        // 3. Find or create conversation
        ConversationEntity conversation = null;
        if (req.getConversationId() != null) {
            conversation = conversationRepository.findById(req.getConversationId()).orElse(null);
        }

        if (conversation == null) {
            conversation = findOrCreateConversation(senderEmail, senderPublicUserId, senderName,
                    receiverEmail, receiverPublicUserId, receiverName);
        }

        Long conversationId = conversation.getId();

        // 4. Update receiver if not yet known
        if (receiverEmail == null || receiverEmail.isBlank()) {
            receiverEmail = senderEmail.equalsIgnoreCase(conversation.getUser1Id())
                    ? conversation.getUser2Id()
                    : conversation.getUser1Id();
            receiverName = resolveUserName(receiverEmail);
        }

        // 5. Persist message in database
        MessageEntity messageEntity = new MessageEntity(
                conversationId,
                senderEmail,
                senderName,
                receiverEmail != null ? receiverEmail : "unknown",
                receiverName,
                req.getContent().trim(),
                req.getAttachment()
        );
        messageEntity = messageRepository.save(messageEntity);

        // 6. Update conversation entity
        conversation.addMessage(senderEmail, senderName, receiverEmail, req.getContent().trim(), "outgoing", req.getAttachment());
        conversation.setPreview(req.getContent().trim());
        conversation.setDate("Just now");
        conversation.setUnread(true);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // 7. Construct DTO for dispatch
        ChatMessageDto outDto = new ChatMessageDto(
                "msg-" + messageEntity.getId(),
                conversationId,
                senderEmail,
                senderName,
                receiverEmail,
                receiverName,
                messageEntity.getContent(),
                "outgoing",
                messageEntity.getAttachment(),
                "SENT"
        );
        outDto.setSentAt(LocalDateTime.now().toString());
        outDto.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));
        outDto.setType(req.getType() != null ? req.getType() : "CHAT");

        // 8. Deliver via WebSocket in real-time to all receiver and sender channels
        deliverRealtime(outDto, senderEmail, senderPublicUserId, senderName,
                receiverEmail, receiverPublicUserId, receiverName, conversationId);

        return outDto;
    }

    private void deliverRealtime(ChatMessageDto outDto,
                                 String senderEmail, String senderPublicId, String senderName,
                                 String receiverEmail, String receiverPublicId, String receiverName,
                                 Long conversationId) {
        try {
            // A. Broadcast to conversation room topics
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, outDto);
            messagingTemplate.convertAndSend("/topic/messages/" + conversationId, outDto);

            // B. Send to receiver's private queue destinations
            Set<String> receiverTargets = new LinkedHashSet<>();
            if (receiverEmail != null && !receiverEmail.isBlank()) receiverTargets.add(receiverEmail);
            if (receiverPublicId != null && !receiverPublicId.isBlank()) receiverTargets.add(receiverPublicId);
            if (receiverName != null && !receiverName.isBlank()) receiverTargets.add(receiverName);

            for (String target : receiverTargets) {
                try {
                    messagingTemplate.convertAndSendToUser(target, "/queue/messages", outDto);
                } catch (Exception ignored) {}
            }

            // C. Dedicated pub-sub direct user topics (bypasses UserDestinationResolver for guaranteed receipt)
            if (receiverEmail != null && !receiverEmail.isBlank()) {
                messagingTemplate.convertAndSend("/topic/user." + receiverEmail.toLowerCase(Locale.ROOT) + ".messages", outDto);
            }
            if (receiverPublicId != null && !receiverPublicId.isBlank()) {
                messagingTemplate.convertAndSend("/topic/user." + receiverPublicId + ".messages", outDto);
            }

            // D. Sender confirmation & multi-device sync
            Set<String> senderTargets = new LinkedHashSet<>();
            if (senderEmail != null && !senderEmail.isBlank()) senderTargets.add(senderEmail);
            if (senderPublicId != null && !senderPublicId.isBlank()) senderTargets.add(senderPublicId);

            for (String target : senderTargets) {
                try {
                    messagingTemplate.convertAndSendToUser(target, "/queue/messages", outDto);
                } catch (Exception ignored) {}
            }
            if (senderEmail != null && !senderEmail.isBlank()) {
                messagingTemplate.convertAndSend("/topic/user." + senderEmail.toLowerCase(Locale.ROOT) + ".messages", outDto);
            }
            if (senderPublicId != null && !senderPublicId.isBlank()) {
                messagingTemplate.convertAndSend("/topic/user." + senderPublicId + ".messages", outDto);
            }

            log.info("[WebSocket] Real-time message #{} delivered to conversation {} (sender: {}, receiver: {})",
                    outDto.getId(), conversationId, senderEmail, receiverEmail);
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

    private ConversationEntity findOrCreateConversation(String senderEmail, String senderPublicId, String senderName,
                                                        String receiverEmail, String receiverPublicId, String receiverName) {
        List<String> userAKeys = Arrays.asList(senderEmail, senderPublicId);
        List<String> userBKeys = Arrays.asList(receiverEmail, receiverPublicId, receiverName);

        // 1. Try finding conversation between userA and userB across any pair of identifiers
        for (String keyA : userAKeys) {
            if (keyA == null || keyA.isBlank()) continue;
            for (String keyB : userBKeys) {
                if (keyB == null || keyB.isBlank()) continue;
                Optional<ConversationEntity> existing = conversationRepository.findByUser1IdAndUser2Id(keyA, keyB);
                if (existing.isPresent()) return existing.get();

                existing = conversationRepository.findByUser1IdAndUser2Id(keyB, keyA);
                if (existing.isPresent()) return existing.get();
            }
        }

        // 2. Try matching by receiver name in all existing conversations
        if (receiverName != null && !receiverName.isBlank()) {
            Optional<ConversationEntity> byName = conversationRepository.findFirstByNameIgnoreCase(receiverName.trim());
            if (byName.isPresent()) {
                ConversationEntity conv = byName.get();
                if (conv.getUser1Id() == null) conv.setUser1Id(senderEmail);
                if (conv.getUser2Id() == null) conv.setUser2Id(receiverEmail);
                return conversationRepository.save(conv);
            }
        }

        // 3. Create canonical new conversation
        ConversationEntity conv = new ConversationEntity();
        conv.setUser1Id(senderEmail != null ? senderEmail : senderPublicId);
        conv.setUser1Name(senderName);
        conv.setUser2Id(receiverEmail != null ? receiverEmail : receiverPublicId);
        conv.setUser2Name(receiverName != null ? receiverName : "Cinema Professional");
        conv.setName(receiverName != null ? receiverName : "Cinema Professional");
        conv.setCategory("Direct");
        conv.setMailbox("Focused");
        conv.setPreview("Start of conversation");
        conv.setDate("Just now");
        return conversationRepository.save(conv);
    }

    public Optional<UserAccountEntity> resolveUserAccount(String identifier) {
        if (identifier == null || identifier.isBlank()) return Optional.empty();
        String clean = identifier.trim();

        // 1. By email
        Optional<UserAccountEntity> byEmail = userAccountRepository.findByEmail(clean.toLowerCase(Locale.ROOT));
        if (byEmail.isPresent()) return byEmail;

        // 2. By public user ID
        Optional<UserAccountEntity> byPublicId = userAccountRepository.findByPublicUserId(clean);
        if (byPublicId.isPresent()) return byPublicId;

        // 3. By profile user ID
        Optional<ProfileEntity> profile = profileRepository.findByUserId(clean);
        if (profile.isPresent() && profile.get().getEmail() != null) {
            Optional<UserAccountEntity> byProfEmail = userAccountRepository.findByEmail(profile.get().getEmail().toLowerCase(Locale.ROOT));
            if (byProfEmail.isPresent()) return byProfEmail;
        }

        // 4. By exact or case-insensitive name match in UserAccounts
        Optional<UserAccountEntity> byName = userAccountRepository.findByName(clean);
        if (byName.isPresent()) return byName;

        // 5. By partial name match in UserAccounts
        Optional<UserAccountEntity> byAnyName = userAccountRepository.findAll().stream()
                .filter(u -> u.getName() != null && (u.getName().equalsIgnoreCase(clean)
                        || clean.toLowerCase().contains(u.getName().toLowerCase())
                        || u.getName().toLowerCase().contains(clean.toLowerCase())))
                .findFirst();
        if (byAnyName.isPresent()) return byAnyName;

        // 6. By Profile name match
        Optional<ProfileEntity> profByName = profileRepository.findByName(clean);
        if (profByName.isPresent() && profByName.get().getEmail() != null) {
            Optional<UserAccountEntity> byProfName = userAccountRepository.findByEmail(profByName.get().getEmail().toLowerCase(Locale.ROOT));
            if (byProfName.isPresent()) return byProfName;
        }

        // 7. If clean is "me", fallback to primary profile account
        if ("me".equalsIgnoreCase(clean)) {
            return userAccountRepository.findByEmail("pro@truehunt.in")
                    .or(() -> userAccountRepository.findAll().stream().findFirst());
        }

        return Optional.empty();
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
        Optional<UserAccountEntity> account = resolveUserAccount(identifier);
        if (account.isPresent()) return account.get().getName();

        Optional<ProfileEntity> profile = profileRepository.findByUserId(identifier);
        if (profile.isPresent()) return profile.get().getName();

        return identifier;
    }
}
