package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.model.ConversationEntity;
import com.truehunt.social.repository.ConversationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationRepository conversationRepository;

    public ConversationController(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConversations(
            @RequestParam(required = false) String mailbox
    ) {
        List<ConversationEntity> list;
        if (mailbox != null && !mailbox.isBlank()) {
            list = conversationRepository.findAllByMailboxOrderByUpdatedAtDesc(mailbox);
        } else {
            list = conversationRepository.findAllByOrderByUpdatedAtDesc();
        }

        List<Map<String, Object>> dtos = list.stream()
                .map(ConversationEntity::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConversation(@PathVariable Long id) {
        Optional<ConversationEntity> conv = conversationRepository.findById(id);
        return conv.map(c -> ResponseEntity.ok(ApiResponse.success(c.toDto())))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Conversation not found")));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMessage(
            @PathVariable Long id,
            @RequestBody Map<String, Object> req
    ) {
        Optional<ConversationEntity> opt = conversationRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Conversation not found"));
        }

        String content = (String) req.get("content");
        String direction = (String) req.getOrDefault("direction", "outgoing");
        Map<String, Object> attachment = req.containsKey("attachment") && req.get("attachment") instanceof Map
                ? (Map<String, Object>) req.get("attachment")
                : null;

        ConversationEntity conv = opt.get();
        conv.addMessage(content, direction, attachment);
        ConversationEntity saved = conversationRepository.save(conv);

        return ResponseEntity.ok(ApiResponse.success(saved.toDto()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createConversation(@RequestBody Map<String, Object> req) {
        ConversationEntity conv = new ConversationEntity();
        String recipient = (String) req.get("recipient");
        String messageText = (String) req.get("messageText");

        conv.setName(recipient != null && !recipient.isBlank() ? recipient : "New Connection");
        conv.setInitials(conv.getName().substring(0, Math.min(2, conv.getName().length())).toUpperCase());
        conv.setColor("blue");
        conv.setCategory("Connections");
        conv.setSubject("Direct Message");
        conv.setDescription(messageText);
        conv.setPreview(messageText);
        conv.setMailbox("Focused");
        conv.addMessage(messageText, "outgoing", null);

        ConversationEntity saved = conversationRepository.save(conv);
        return ResponseEntity.ok(ApiResponse.success(saved.toDto()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAsRead(@PathVariable Long id) {
        Optional<ConversationEntity> opt = conversationRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Conversation not found"));
        }

        ConversationEntity conv = opt.get();
        conv.setUnread(false);
        ConversationEntity saved = conversationRepository.save(conv);
        return ResponseEntity.ok(ApiResponse.success(saved.toDto()));
    }

    @PatchMapping("/{id}/star")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleStar(@PathVariable Long id) {
        Optional<ConversationEntity> opt = conversationRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Conversation not found"));
        }

        ConversationEntity conv = opt.get();
        // Toggle starred in DTO
        boolean currentlyStarred = "Starred".equalsIgnoreCase(conv.getCategory());
        conv.setCategory(currentlyStarred ? "Connections" : "Starred");
        ConversationEntity saved = conversationRepository.save(conv);

        Map<String, Object> dto = saved.toDto();
        dto.put("starred", !currentlyStarred);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PatchMapping("/{id}/mailbox")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMailbox(
            @PathVariable Long id,
            @RequestBody Map<String, String> req
    ) {
        Optional<ConversationEntity> opt = conversationRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Conversation not found"));
        }

        String newMailbox = req.get("mailbox");
        if (newMailbox == null || newMailbox.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "Mailbox cannot be empty"));
        }

        ConversationEntity conv = opt.get();
        conv.setMailbox(newMailbox);
        ConversationEntity saved = conversationRepository.save(conv);
        return ResponseEntity.ok(ApiResponse.success(saved.toDto()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteConversation(@PathVariable Long id) {
        if (conversationRepository.existsById(id)) {
            conversationRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Conversation deleted successfully"));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Conversation not found"));
    }
}
