package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.ChatMessageDto;
import com.truehunt.social.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private Principal resolveCallerPrincipal(Principal principal, String userEmail, String userId) {
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal;
        }
        String identifier = resolveCallerIdentifier(principal, userEmail, userId);
        return identifier != null ? () -> identifier : null;
    }

    private String resolveCallerIdentifier(Principal principal, String userEmail, String userId) {
        if (userId != null && !userId.isBlank()) {
            return userId.trim();
        }
        if (userEmail != null && !userEmail.isBlank()) {
            return userEmail.trim();
        }
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName().trim();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return null;
    }

    /**
     * REST endpoint for sending messages with full fallback support
     */
    @PostMapping("/api/messages")
    @ResponseBody
    public ResponseEntity<ApiResponse<ChatMessageDto>> sendRestMessage(
            @RequestBody ChatMessageDto message,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Principal principal
    ) {
        Principal callerPrincipal = resolveCallerPrincipal(principal, userEmail, userId);
        ChatMessageDto result = messageService.processAndSendMessage(message, callerPrincipal);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * REST endpoint to retrieve all messages in a conversation
     */
    @GetMapping("/api/messages/{conversationId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConversationMessages(
            @PathVariable Long conversationId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Principal principal
    ) {
        String caller = resolveCallerIdentifier(principal, userEmail, userId);
        List<Map<String, Object>> messages = messageService.getMessagesForConversation(conversationId, caller);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
}
