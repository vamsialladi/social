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
        List<ConversationEntity> list = (mailbox != null && !mailbox.isBlank())
                ? conversationRepository.findAllByMailboxIgnoreCaseOrderByUpdatedAtDesc(mailbox)
                : conversationRepository.findAllByOrderByUpdatedAtDesc();

        if (userEmail != null && !userEmail.isBlank()) {
            final String cleanEmail = userEmail.trim().toLowerCase(Locale.ROOT);
            final Set<String> callerIdentifiers = new HashSet<>();
            callerIdentifiers.add(cleanEmail);

            userAccountRepository.findByEmail(cleanEmail)
                    .or(() -> userAccountRepository.findByPublicUserId(userEmail))
                    .ifPresent(u -> {
                        if (u.getEmail() != null)
                            callerIdentifiers.add(u.getEmail().trim().toLowerCase(Locale.ROOT));
                        if (u.getPublicUserId() != null)
                            callerIdentifiers.add(u.getPublicUserId().trim().toLowerCase(Locale.ROOT));
                        if (u.getId() != null)
                            callerIdentifiers.add(String.valueOf(u.getId()));
                    });

            return list.stream()
                    .filter(c -> {
                        String u1 = c.getUser1Id() != null ? c.getUser1Id().trim().toLowerCase(Locale.ROOT) : "";
                        String u2 = c.getUser2Id() != null ? c.getUser2Id().trim().toLowerCase(Locale.ROOT) : "";
                        String cName = c.getName() != null ? c.getName().trim().toLowerCase(Locale.ROOT) : "";

                        if (u1.isEmpty() && u2.isEmpty())
                            return true;

                        boolean matchU1 = callerIdentifiers.contains(u1);
                        boolean matchU2 = callerIdentifiers.contains(u2);
                        boolean matchName = !cName.isEmpty() && callerIdentifiers.contains(cName);

                        return matchU1 || matchU2 || matchName;
                    })
                    .map(c -> c.toDto(cleanEmail, callerIdentifiers))
                    .collect(Collectors.toList());
        }

        return list.stream()
                .map(ConversationEntity::toDto)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getConversation(String id) {
        return getConversation(id, null);
    }

    public Map<String, Object> getConversation(String id, String userEmail) {
        ConversationEntity entity = findConversationEntity(id);
        if (userEmail != null && !userEmail.isBlank()) {
            final String cleanEmail = userEmail.trim().toLowerCase(Locale.ROOT);
            final Set<String> callerIdentifiers = new HashSet<>();
            callerIdentifiers.add(cleanEmail);

            userAccountRepository.findByEmail(cleanEmail)
                    .or(() -> userAccountRepository.findByPublicUserId(userEmail))
                    .ifPresent(u -> {
                        if (u.getEmail() != null)
                            callerIdentifiers.add(u.getEmail().trim().toLowerCase(Locale.ROOT));
                        if (u.getPublicUserId() != null)
                            callerIdentifiers.add(u.getPublicUserId().trim().toLowerCase(Locale.ROOT));
                        if (u.getId() != null)
                            callerIdentifiers.add(String.valueOf(u.getId()));
                    });
            return entity.toDto(cleanEmail, callerIdentifiers);
        }
        return entity.toDto(userEmail);
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

        com.truehunt.social.dto.ChatMessageDto chatDto = new com.truehunt.social.dto.ChatMessageDto();
        chatDto.setConversationId(entity.getId());
        chatDto.setContent(req.getContent().trim());
        chatDto.setAttachment(req.getAttachment());
        chatDto.setSenderId(userEmail);
        chatDto.setDirection(req.getDirection() != null ? req.getDirection() : "outgoing");

        final String finalEmail = userEmail;
        java.security.Principal principal = finalEmail != null ? () -> finalEmail : null;
        com.truehunt.social.dto.ChatMessageDto sentDto = messageService.processAndSendMessage(chatDto, principal);

        Map<String, Object> dto = entity.toDto(userEmail);
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
        return conversationRepository.save(conversation).toDto(userId, callerIdentifiers);
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
        return conversationRepository.save(conversation).toDto(userId, callerIdentifiers);
    }

    @Transactional
    public Map<String, Object> createConversation(CreateConversationRequest req) {
        return createConversation(req, null);
    }

    @Transactional
    public Map<String, Object> createConversation(CreateConversationRequest req, String userEmail) {
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
                return entity.toDto(userEmail);
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
        return saved.toDto(userEmail);
    }

    @Transactional
    public Map<String, Object> markAsRead(String id) {
        ConversationEntity entity = findConversationEntity(id);
        entity.setUnread(false);
        conversationRepository.save(entity);
        return entity.toDto();
    }

    @Transactional
    public Map<String, Object> starConversation(String id) {
        ConversationEntity entity = findConversationEntity(id);
        entity.setStarred(!entity.isStarred());
        conversationRepository.save(entity);
        return entity.toDto();
    }

    @Transactional
    public Map<String, Object> updateMailbox(String id, String mailbox) {
        ConversationEntity entity = findConversationEntity(id);
        entity.setMailbox(mailbox);
        conversationRepository.save(entity);
        return entity.toDto();
    }

    @Transactional
    public void deleteConversation(String id) {
        try {
            ConversationEntity entity = findConversationEntity(id);
            conversationRepository.delete(entity);
        } catch (Exception e) {
            Long numericId = parseLongId(id);
            if (numericId != null && conversationRepository.existsById(numericId)) {
                conversationRepository.deleteById(numericId);
            }
        }
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
