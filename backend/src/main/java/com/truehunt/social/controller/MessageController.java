package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.ChatMessageDto;
import com.truehunt.social.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Handles real-time messages sent to /app/chat.send
     */
    @MessageMapping("/chat.send")
    public void handleChatMessage(@Payload ChatMessageDto message, Principal principal) {
        messageService.processAndSendMessage(message, principal);
    }

    /**
     * Handles private one-to-one messages sent to /app/chat.private
     */
    @MessageMapping("/chat.private")
    public void handlePrivateMessage(@Payload ChatMessageDto message, Principal principal) {
        messageService.processAndSendMessage(message, principal);
    }

    /**
     * REST endpoint for sending messages with full fallback support
     */
    @PostMapping("/api/messages")
    @ResponseBody
    public ResponseEntity<ApiResponse<ChatMessageDto>> sendRestMessage(
            @RequestBody ChatMessageDto message,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            Principal principal
    ) {
        if (principal == null && userEmail != null && !userEmail.isBlank()) {
            principal = () -> userEmail;
        }
        ChatMessageDto result = messageService.processAndSendMessage(message, principal);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * REST endpoint to retrieve all messages in a conversation
     */
    @GetMapping("/api/messages/{conversationId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConversationMessages(
            @PathVariable Long conversationId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        List<Map<String, Object>> messages = messageService.getMessagesForConversation(conversationId, userEmail);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
}
