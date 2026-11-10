package com.truehunt.social.service;

import com.truehunt.social.dto.CreateConversationRequest;
import com.truehunt.social.dto.SendMessageRequest;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.ConversationEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ConversationRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageService messageService;
    private final UserAccountRepository userAccountRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               MessageService messageService,
                               UserAccountRepository userAccountRepository) {
        this.conversationRepository = conversationRepository;
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
                    .or(() -> userAccountRepository.findByName(userEmail))
                    .or(() -> userAccountRepository.findFirstByNameIgnoreCase(userEmail))
                    .ifPresent(u -> {
                        if (u.getEmail() != null) callerIdentifiers.add(u.getEmail().trim().toLowerCase(Locale.ROOT));
                        if (u.getPublicUserId() != null) callerIdentifiers.add(u.getPublicUserId().trim().toLowerCase(Locale.ROOT));
                        if (u.getName() != null) callerIdentifiers.add(u.getName().trim().toLowerCase(Locale.ROOT));
                    });

            return list.stream()
                    .filter(c -> {
                        String u1 = c.getUser1Id() != null ? c.getUser1Id().trim().toLowerCase(Locale.ROOT) : "";
                        String u2 = c.getUser2Id() != null ? c.getUser2Id().trim().toLowerCase(Locale.ROOT) : "";
                        String n1 = c.getUser1Name() != null ? c.getUser1Name().trim().toLowerCase(Locale.ROOT) : "";
                        String n2 = c.getUser2Name() != null ? c.getUser2Name().trim().toLowerCase(Locale.ROOT) : "";
                        String cName = c.getName() != null ? c.getName().trim().toLowerCase(Locale.ROOT) : "";

                        // If unassigned legacy conversation, let it show
                        if (u1.isEmpty() && u2.isEmpty()) return true;

                        boolean matchU1 = callerIdentifiers.contains(u1) || (!n1.isEmpty() && callerIdentifiers.contains(n1));
                        boolean matchU2 = callerIdentifiers.contains(u2) || (!n2.isEmpty() && callerIdentifiers.contains(n2));
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
                    .or(() -> userAccountRepository.findByName(userEmail))
                    .or(() -> userAccountRepository.findFirstByNameIgnoreCase(userEmail))
                    .ifPresent(u -> {
                        if (u.getEmail() != null) callerIdentifiers.add(u.getEmail().trim().toLowerCase(Locale.ROOT));
                        if (u.getPublicUserId() != null) callerIdentifiers.add(u.getPublicUserId().trim().toLowerCase(Locale.ROOT));
                        if (u.getName() != null) callerIdentifiers.add(u.getName().trim().toLowerCase(Locale.ROOT));
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
            if (entity != null) return entity.toDto(userEmail);
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
            if (opt.isPresent()) return opt.get();
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

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "TH";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
    }
}
