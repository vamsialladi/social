package com.truehunt.social.service;

import com.truehunt.social.dto.CreateConversationRequest;
import com.truehunt.social.dto.SendMessageRequest;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.ConversationEntity;
import com.truehunt.social.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public List<Map<String, Object>> getConversations(String mailbox) {
        List<ConversationEntity> list = (mailbox != null && !mailbox.isBlank())
                ? conversationRepository.findAllByMailboxIgnoreCaseOrderByUpdatedAtDesc(mailbox)
                : conversationRepository.findAllByOrderByUpdatedAtDesc();

        return list.stream()
                .map(ConversationEntity::toDto)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getConversation(String id) {
        ConversationEntity entity = findConversationEntity(id);
        return entity.toDto();
    }

    @Transactional
    public Map<String, Object> sendMessage(String id, SendMessageRequest req) {
        if (req == null || req.getContent() == null || req.getContent().trim().isEmpty()) {
            throw new BadRequestException("Message content cannot be empty.");
        }
        ConversationEntity entity = findConversationEntity(id);

        entity.addMessage(req.getContent(), req.getDirection() != null ? req.getDirection() : "outgoing", req.getAttachment());
        conversationRepository.save(entity);

        Map<String, Object> dto = entity.toDto();
        dto.put("content", req.getContent());
        List<Map<String, Object>> thread = entity.getThread();
        dto.put("message", thread.isEmpty() ? null : thread.get(thread.size() - 1));
        return dto;
    }

    @Transactional
    public Map<String, Object> createConversation(CreateConversationRequest req) {
        ConversationEntity c = new ConversationEntity();
        c.setName(req.getRecipient());
        c.setInitials(getInitials(req.getRecipient()));
        c.setColor("blue");
        c.setPreview(req.getMessageText());
        c.setDate("Just now");
        c.setCategory("Direct");
        c.setMailbox("Focused");
        c.setUnread(false);

        Map<String, Object> msg = new HashMap<>();
        msg.put("id", "msg-" + UUID.randomUUID().toString().substring(0, 8));
        msg.put("content", req.getMessageText());
        msg.put("direction", "outgoing");
        c.setThread(List.of(msg));

        ConversationEntity saved = conversationRepository.save(c);
        return saved.toDto();
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
