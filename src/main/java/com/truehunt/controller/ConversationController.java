package com.truehunt.controller;

import com.truehunt.config.UserPrincipal;
import com.truehunt.dto.request.SendMessageRequest;
import com.truehunt.dto.response.ApiResponse;
import com.truehunt.dto.response.ConversationResponse;
import com.truehunt.dto.response.MessageResponse;
import com.truehunt.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<ConversationResponse> conversations = messageService.listConversations(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.of(conversations));
    }

    @PostMapping("/direct/{otherUserId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getOrCreateDirectConversation(
            @PathVariable String otherUserId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        ConversationResponse conv = messageService.getOrCreateDirectConversation(currentUser.getId(), otherUserId);
        return ResponseEntity.ok(ApiResponse.of(conv));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable String id,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", defaultValue = "30") int limit,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<MessageResponse> messages = messageService.getMessages(id, currentUser.getId(), cursor, limit);
        String nextCursor = null;
        if (!messages.isEmpty() && messages.size() >= limit) {
            nextCursor = messages.get(messages.size() - 1).getCreatedAt().toString();
        }

        return ResponseEntity.ok(ApiResponse.of(messages, nextCursor));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody SendMessageRequest request) {

        MessageResponse response = messageService.sendMessage(id, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }
}
