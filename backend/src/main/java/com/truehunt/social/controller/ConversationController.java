package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.CreateConversationRequest;
import com.truehunt.social.dto.SendMessageRequest;
import com.truehunt.social.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConversations(
            @RequestParam(required = false) String mailbox,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        String caller = (userEmail != null && !userEmail.isBlank()) ? userEmail : userId;
        return ResponseEntity.ok(ApiResponse.success(conversationService.getConversations(mailbox, caller)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConversation(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        String caller = (userEmail != null && !userEmail.isBlank()) ? userEmail : userId;
        return ResponseEntity.ok(ApiResponse.success(conversationService.getConversation(id, caller)));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMessage(
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequest req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        String caller = (userEmail != null && !userEmail.isBlank()) ? userEmail : userId;
        return ResponseEntity.ok(ApiResponse.success(conversationService.sendMessage(id, req, caller)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createConversation(
            @Valid @RequestBody CreateConversationRequest req,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        String caller = (userEmail != null && !userEmail.isBlank()) ? userEmail : userId;
        return ResponseEntity.ok(ApiResponse.success(conversationService.createConversation(req, caller)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAsRead(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(conversationService.markAsRead(id)));
    }

    @PatchMapping("/{id}/star")
    public ResponseEntity<ApiResponse<Map<String, Object>>> starConversation(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(conversationService.starConversation(id)));
    }

    @PatchMapping("/{id}/mailbox")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMailbox(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> req
    ) {
        String mailbox = (req != null && req.containsKey("mailbox")) ? req.get("mailbox") : "Focused";
        return ResponseEntity.ok(ApiResponse.success(conversationService.updateMailbox(id, mailbox)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteConversation(@PathVariable String id) {
        conversationService.deleteConversation(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Conversation deleted successfully", "id", id)));
    }
}
