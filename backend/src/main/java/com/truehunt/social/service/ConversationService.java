package com.truehunt.social.service;

import com.truehunt.social.dto.CreateConversationRequest;
import com.truehunt.social.dto.SendMessageRequest;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.ConversationEntity;
import com.truehunt.social.model.MessageEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ConversationRepository;
import com.truehunt.social.repository.MessageRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageService messageService;
    private final UserAccountRepository userAccountRepository;

    public ConversationService(ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            MessageService messageService,
            UserAccountRepository userAccountRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.messageService = messageService;
        this.userAccountRepository = userAccountRepository;
    }

    public List<Map<String, Object>> getConversations(String mailbox) {
        return getConversations(mailbox, null);
    }

    public List<Map<String, Object>> getConversations(String mailbox, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new UnauthorizedException("A valid sign-in is required to view conversations.");
        }
        List<ConversationEntity> list = (mailbox != null && !mailbox.isBlank())
                ? conversationRepository.findAllByMailboxIgnoreCaseOrderByUpdatedAtDesc(mailbox)
                : conversationRepository.findAllByOrderByUpdatedAtDesc();

        final String cleanEmail = userEmail.trim().toLowerCase(Locale.ROOT);
        final Set<String> callerIdentifiers = resolveCallerIdentifiers(userEmail);
        return list.stream()
                .filter(c -> callerIdentifiers.contains(normalizeIdentity(c.getUser1Id()))
                        || callerIdentifiers.contains(normalizeIdentity(c.getUser2Id())))
                .map(c -> toEnrichedDto(c, cleanEmail, callerIdentifiers))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getConversation(String id) {
        return getConversation(id, null);
    }

    public Map<String, Object> getConversation(String id, String userEmail) {
        ConversationEntity entity = findConversationEntity(id);
        Set<String> callerIdentifiers = requireConversationParticipant(entity, userEmail);
        return toEnrichedDto(entity, userEmail, callerIdentifiers);
    }

    @Transactional
    public Map<String, Object> sendMessage(String id, SendMessageRequest req) {
        return sendMessage(id, req, null);
    }

    @Transactional
    public Map<String, Object> sendMessage(String id, SendMessageRequest req, String userEmail) {
        if (req == null || req.getContent() == null || req.getContent().trim().isEmpty()) {
            throw new BadRequestException("Message content cannot be empty.");
        }
        ConversationEntity entity = findConversationEntity(id);
        requireConversationParticipant(entity, userEmail);

        com.truehunt.social.dto.ChatMessageDto chatDto = new com.truehunt.social.dto.ChatMessageDto();
        chatDto.setConversationId(entity.getId());
        chatDto.setContent(req.getContent().trim());
        chatDto.setAttachment(req.getAttachment());
        chatDto.setSenderId(userEmail);
        chatDto.setDirection(req.getDirection() != null ? req.getDirection() : "outgoing");

        final String finalEmail = userEmail;
        java.security.Principal principal = finalEmail != null ? () -> finalEmail : null;
        com.truehunt.social.dto.ChatMessageDto sentDto = messageService.processAndSendMessage(chatDto, principal);

        ConversationEntity refreshedEntity = conversationRepository.findById(entity.getId()).orElse(entity);
        Map<String, Object> dto = toEnrichedDto(refreshedEntity, userEmail);
        dto.put("content", req.getContent());
        dto.put("message", sentDto);
        return dto;
    }

    @Transactional
    public Map<String, Object> editMessage(String conversationId, String messageId, String content, String userId) {
        String cleanContent = content == null ? "" : content.trim();
        if (cleanContent.isEmpty()) {
            throw new BadRequestException("Message content cannot be empty.");
        }

        ConversationEntity conversation = findConversationEntity(conversationId);
        Set<String> callerIdentifiers = requireConversationParticipant(conversation, userId);
        List<Map<String, Object>> thread = conversation.getThread();
        int messageIndex = -1;
        for (int index = 0; index < thread.size(); index++) {
            if (messageId.equals(String.valueOf(thread.get(index).get("id")))) {
                messageIndex = index;
                break;
            }
        }
        if (messageIndex < 0) {
            throw new ResourceNotFoundException("Message", "id", messageId);
        }

        Map<String, Object> message = thread.get(messageIndex);
        String senderId = String.valueOf(message.getOrDefault("senderId", ""));
        if (!callerIdentifiers.contains(normalizeIdentity(senderId))) {
            throw new UnauthorizedException("You can only edit your own messages.");
        }
        if (Boolean.TRUE.equals(message.get("edited"))) {
            throw new BadRequestException("A message can only be edited once.");
        }

        MessageEntity storedMessage = findStoredMessage(conversation.getId(), message, senderId);
        if (storedMessage != null) {
            if (!callerIdentifiers.contains(normalizeIdentity(storedMessage.getSenderId()))) {
                throw new UnauthorizedException("You can only edit your own messages.");
            }
            storedMessage.setContent(cleanContent);
            messageRepository.save(storedMessage);
        }

        message.put("content", cleanContent);
        message.put("edited", true);
        conversation.setThread(thread);
        if (messageIndex == thread.size() - 1) {
            conversation.setPreview(cleanContent);
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        return toEnrichedDto(conversationRepository.save(conversation), userId, callerIdentifiers);
    }

    @Transactional
    public Map<String, Object> clearMessages(String conversationId, String userId) {
        ConversationEntity conversation = findConversationEntity(conversationId);
        Set<String> callerIdentifiers = requireConversationParticipant(conversation, userId);
        messageRepository.deleteAll(messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversation.getId()));
        conversation.setThread(List.of());
        conversation.setPreview("Start a conversation");
        conversation.setDate("Now");
        conversation.setUnread(false);
        conversation.setUpdatedAt(LocalDateTime.now());
        return toEnrichedDto(conversationRepository.save(conversation), userId, callerIdentifiers);
    }

    @Transactional
    public Map<String, Object> createConversation(CreateConversationRequest req) {
        return createConversation(req, null);
    }

    @Transactional
    public Map<String, Object> createConversation(CreateConversationRequest req, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new UnauthorizedException("A valid sign-in is required to start a conversation.");
        }
        com.truehunt.social.dto.ChatMessageDto chatDto = new com.truehunt.social.dto.ChatMessageDto();
        chatDto.setContent(req.getMessageText() != null ? req.getMessageText().trim() : "Hi");
        chatDto.setReceiverId(req.getRecipient());
        chatDto.setReceiverName(req.getRecipient());
        chatDto.setSenderId(userEmail);

        final String finalEmail = userEmail;
        java.security.Principal principal = finalEmail != null ? () -> finalEmail : null;
        com.truehunt.social.dto.ChatMessageDto sentDto = messageService.processAndSendMessage(chatDto, principal);

        if (sentDto.getConversationId() != null) {
            ConversationEntity entity = conversationRepository.findById(sentDto.getConversationId()).orElse(null);
            if (entity != null)
                return toEnrichedDto(entity, userEmail);
        }

        ConversationEntity c = new ConversationEntity();
        c.setName(req.getRecipient());
        c.setInitials(getInitials(req.getRecipient()));
        c.setColor("blue");
        c.setPreview(req.getMessageText());
        c.setDate("Just now");
        c.setCategory("Direct");
        c.setMailbox("Focused");
        c.setUnread(false);
        c.setUser1Id(userEmail);
        c.setUser2Id(req.getRecipient());

        Map<String, Object> msg = new HashMap<>();
        msg.put("id", "msg-" + UUID.randomUUID().toString().substring(0, 8));
        msg.put("content", req.getMessageText());
        msg.put("direction", "outgoing");
        c.setThread(List.of(msg));

        ConversationEntity saved = conversationRepository.save(c);
        return toEnrichedDto(saved, userEmail);
    }

    @Transactional
    public Map<String, Object> markAsRead(String id) {
        return markAsRead(id, null);
    }

    @Transactional
    public Map<String, Object> markAsRead(String id, String userEmail) {
        ConversationEntity entity = findConversationEntity(id);
        requireConversationParticipant(entity, userEmail);
        entity.setUnread(false);
        conversationRepository.save(entity);
        return toEnrichedDto(entity, userEmail);
    }

    @Transactional
    public Map<String, Object> starConversation(String id) {
        return starConversation(id, null);
    }

    @Transactional
    public Map<String, Object> starConversation(String id, String userEmail) {
        ConversationEntity entity = findConversationEntity(id);
        requireConversationParticipant(entity, userEmail);
        entity.setStarred(!entity.isStarred());
        conversationRepository.save(entity);
        return toEnrichedDto(entity, userEmail);
    }

    @Transactional
    public Map<String, Object> updateMailbox(String id, String mailbox) {
        return updateMailbox(id, mailbox, null);
    }

    @Transactional
    public Map<String, Object> updateMailbox(String id, String mailbox, String userEmail) {
        ConversationEntity entity = findConversationEntity(id);
        requireConversationParticipant(entity, userEmail);
        entity.setMailbox(mailbox);
        conversationRepository.save(entity);
        return toEnrichedDto(entity, userEmail);
    }

    @Transactional
    public void deleteConversation(String id) {
        deleteConversation(id, null);
    }

    @Transactional
    public void deleteConversation(String id, String userEmail) {
        ConversationEntity entity = findConversationEntity(id);
        requireConversationParticipant(entity, userEmail);
        conversationRepository.delete(entity);
    }

    private ConversationEntity findConversationEntity(String id) {
        Long numericId = parseLongId(id);
        if (numericId != null) {
            Optional<ConversationEntity> opt = conversationRepository.findById(numericId);
            if (opt.isPresent())
                return opt.get();
        }
        return conversationRepository.findFirstByNameIgnoreCase(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", id));
    }

    private Long parseLongId(String id) {
        try {
            return Long.parseLong(id);
        } catch (Exception e) {
            return null;
        }
    }

    private Set<String> requireConversationParticipant(ConversationEntity conversation, String userId) {
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("A valid sign-in is required to manage messages.");
        }

        Set<String> identifiers = new HashSet<>();
        identifiers.add(normalizeIdentity(userId));
        Optional<UserAccountEntity> account = userAccountRepository.findByEmail(userId.trim().toLowerCase(Locale.ROOT))
                .or(() -> userAccountRepository.findByPublicUserId(userId.trim()));
        if (account.isEmpty()) {
            try {
                account = userAccountRepository.findById(Long.parseLong(userId.trim()));
            } catch (NumberFormatException ignored) {
                account = Optional.empty();
            }
        }
        account.ifPresent(user -> {
            if (user.getEmail() != null) identifiers.add(normalizeIdentity(user.getEmail()));
            if (user.getPublicUserId() != null) identifiers.add(normalizeIdentity(user.getPublicUserId()));
            if (user.getId() != null) identifiers.add(normalizeIdentity(String.valueOf(user.getId())));
        });

        boolean isParticipant = identifiers.contains(normalizeIdentity(conversation.getUser1Id()))
                || identifiers.contains(normalizeIdentity(conversation.getUser2Id()));
        if (!isParticipant) {
            throw new UnauthorizedException("You cannot manage messages in this conversation.");
        }
        return identifiers;
    }

    private MessageEntity findStoredMessage(Long conversationId, Map<String, Object> message, String senderId) {
        Object storedId = message.get("messageId");
        if (storedId != null) {
            try {
                MessageEntity storedMessage = messageRepository.findById(Long.parseLong(String.valueOf(storedId)))
                        .orElse(null);
                if (storedMessage != null && conversationId.equals(storedMessage.getConversationId())) {
                    return storedMessage;
                }
            } catch (NumberFormatException ignored) {
                // Fall back to the message content for older conversation records.
            }
        }

        String originalContent = String.valueOf(message.getOrDefault("content", ""));
        return messageRepository.findAllByConversationIdOrderByCreatedAtDesc(conversationId).stream()
                .filter(candidate -> normalizeIdentity(candidate.getSenderId()).equals(normalizeIdentity(senderId)))
                .filter(candidate -> Objects.equals(candidate.getContent(), originalContent))
                .findFirst()
                .orElse(null);
    }

    private Set<String> resolveCallerIdentifiers(String userEmail) {
        Set<String> callerIdentifiers = new HashSet<>();
        if (userEmail == null || userEmail.isBlank()) {
            return callerIdentifiers;
        }
        String cleanEmail = userEmail.trim().toLowerCase(Locale.ROOT);
        callerIdentifiers.add(cleanEmail);
        userAccountRepository.findByEmail(cleanEmail)
                .or(() -> userAccountRepository.findByPublicUserId(userEmail.trim()))
                .ifPresent(u -> {
                    if (u.getEmail() != null)
                        callerIdentifiers.add(u.getEmail().trim().toLowerCase(Locale.ROOT));
                    if (u.getPublicUserId() != null)
                        callerIdentifiers.add(u.getPublicUserId().trim().toLowerCase(Locale.ROOT));
                    if (u.getId() != null)
                        callerIdentifiers.add(String.valueOf(u.getId()));
                });
        return callerIdentifiers;
    }

    private Map<String, Object> toEnrichedDto(ConversationEntity entity, String currentUserId) {
        Set<String> callerIdentifiers = (currentUserId != null && !currentUserId.isBlank())
                ? resolveCallerIdentifiers(currentUserId)
                : Collections.emptySet();
        return toEnrichedDto(entity, currentUserId, callerIdentifiers);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toEnrichedDto(ConversationEntity entity,
                                              String currentUserId,
                                              Set<String> callerIdentifiers) {
        Map<String, Object> dto = entity.toDto(currentUserId, callerIdentifiers);

        String liveUser1Name = resolveLiveParticipantName(entity.getUser1Id());
        String liveUser2Name = resolveLiveParticipantName(entity.getUser2Id());

        String effectiveUser1Name = (liveUser1Name != null && !liveUser1Name.isBlank())
                ? liveUser1Name
                : entity.getUser1Name();
        String effectiveUser2Name = (liveUser2Name != null && !liveUser2Name.isBlank())
                ? liveUser2Name
                : entity.getUser2Name();

        if (effectiveUser1Name != null && !effectiveUser1Name.isBlank()) {
            dto.put("user1Name", effectiveUser1Name);
        }
        if (effectiveUser2Name != null && !effectiveUser2Name.isBlank()) {
            dto.put("user2Name", effectiveUser2Name);
        }

        Set<String> cleanIdentifiers = new HashSet<>();
        if (currentUserId != null && !currentUserId.isBlank()) {
            cleanIdentifiers.add(normalizeIdentity(currentUserId));
        }
        if (callerIdentifiers != null) {
            callerIdentifiers.stream()
                    .filter(Objects::nonNull)
                    .map(this::normalizeIdentity)
                    .filter(s -> !s.isEmpty())
                    .forEach(cleanIdentifiers::add);
        }

        String u1IdClean = normalizeIdentity(entity.getUser1Id());
        String u2IdClean = normalizeIdentity(entity.getUser2Id());
        boolean isUser1 = !u1IdClean.isEmpty() && cleanIdentifiers.contains(u1IdClean);
        boolean isUser2 = !u2IdClean.isEmpty() && cleanIdentifiers.contains(u2IdClean);

        if (isUser1 && effectiveUser2Name != null && !effectiveUser2Name.isBlank()) {
            dto.put("name", effectiveUser2Name);
            dto.put("initials", getInitials(effectiveUser2Name));
        } else if (isUser2 && effectiveUser1Name != null && !effectiveUser1Name.isBlank()) {
            dto.put("name", effectiveUser1Name);
            dto.put("initials", getInitials(effectiveUser1Name));
        } else if (!isUser1 && !isUser2 && liveUser2Name != null && !liveUser2Name.isBlank()) {
            dto.put("name", liveUser2Name);
            dto.put("initials", getInitials(liveUser2Name));
        }

        Object rawThread = dto.get("thread");
        if (rawThread instanceof List<?> rawList && !rawList.isEmpty()) {
            List<Map<String, Object>> enrichedThread = new ArrayList<>(rawList.size());
            for (Object entry : rawList) {
                if (!(entry instanceof Map<?, ?> rawMap)) continue;
                Map<String, Object> msg = new HashMap<>((Map<String, Object>) rawMap);
                Object senderIdObj = msg.get("senderId");
                if (senderIdObj != null) {
                    String msgSenderId = String.valueOf(senderIdObj);
                    String msgSenderClean = normalizeIdentity(msgSenderId);
                    if (!msgSenderClean.isEmpty() && msgSenderClean.equals(u1IdClean)
                            && effectiveUser1Name != null && !effectiveUser1Name.isBlank()) {
                        msg.put("senderName", effectiveUser1Name);
                    } else if (!msgSenderClean.isEmpty() && msgSenderClean.equals(u2IdClean)
                            && effectiveUser2Name != null && !effectiveUser2Name.isBlank()) {
                        msg.put("senderName", effectiveUser2Name);
                    } else {
                        String resolvedSender = resolveLiveParticipantName(msgSenderId);
                        if (resolvedSender != null && !resolvedSender.isBlank()) {
                            msg.put("senderName", resolvedSender);
                        }
                    }
                }
                enrichedThread.add(msg);
            }
            dto.put("thread", enrichedThread);
        }

        return dto;
    }

    private String resolveLiveParticipantName(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        String live = messageService.resolveLatestUserName(identifier);
        if (live != null && !live.isBlank()) {
            return live.trim();
        }
        Optional<UserAccountEntity> account = userAccountRepository.findByEmail(normalizeIdentity(identifier))
                .or(() -> userAccountRepository.findByPublicUserId(identifier.trim()));
        if (account.isPresent() && account.get().getName() != null && !account.get().getName().isBlank()) {
            return account.get().getName().trim();
        }
        return null;
    }

    private String normalizeIdentity(String identity) {
        return identity == null ? "" : identity.trim().toLowerCase(Locale.ROOT);
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank())
            return "TH";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1)
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
    }
}
