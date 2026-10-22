package com.social.controller;

import com.social.dto.request.CreateConversationRequest;
import com.social.dto.request.SendMessageRequest;
import com.social.dto.response.ApiResponse;
import com.social.dto.response.ConversationResponse;
import com.social.dto.response.ConversationsPageResponse;
import com.social.dto.response.MessageResponse;
import com.social.dto.response.MessagesPageResponse;
import com.social.service.ConversationService;
import com.social.service.MessageService;
import com.social.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Messaging", description = "Module 7: One-to-one and group conversations between users")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    public ConversationController(ConversationService conversationService,
                                  MessageService messageService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @GetMapping
    @Operation(summary = "List signed-in user's conversations", description = "Returns conversations sorted by lastMessageAt descending with cursor pagination.")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> listConversations(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit,
            @Parameter(hidden = true) @CurrentUser String currentUserId) {
        ConversationsPageResponse page = conversationService.getConversationsForUser(currentUserId, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(page.getConversations(), page.getNextCursor()));
    }

    @PostMapping
    @Operation(summary = "Start or retrieve an existing conversation", description = "Finds existing 1-on-1 conversation or creates a new one.")
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @Parameter(hidden = true) @CurrentUser String currentUserId,
            @RequestBody @Valid CreateConversationRequest request) {
        ConversationResponse response = conversationService.createOrGetConversation(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get conversation by ID", description = "Verifies membership before returning conversation metadata.")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @PathVariable("id") String conversationId,
            @Parameter(hidden = true) @CurrentUser String currentUserId) {
        ConversationResponse conversation = conversationService.getConversationById(conversationId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Read messages with cursor pagination", description = "Verifies user is a participant before returning messages (newest first).")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable("id") String conversationId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit,
            @Parameter(hidden = true) @CurrentUser String currentUserId) {
        MessagesPageResponse response = messageService.getMessages(conversationId, currentUserId, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(response.getMessages(), response.getNextCursor()));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Send a message", description = "Validates membership & content, updates lastMessageAt, and triggers recipient notification.")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable("id") String conversationId,
            @RequestBody SendMessageRequest request,
            @Parameter(hidden = true) @CurrentUser String currentUserId) {
        MessageResponse response = messageService.sendMessage(conversationId, currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark messages as read", description = "Sets readAt on unread messages in the conversation for the current user.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAsRead(
            @PathVariable("id") String conversationId,
            @Parameter(hidden = true) @CurrentUser String currentUserId) {
        int count = messageService.markAsRead(conversationId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "conversationId", conversationId,
                "markedReadCount", count
        )));
    }
}
