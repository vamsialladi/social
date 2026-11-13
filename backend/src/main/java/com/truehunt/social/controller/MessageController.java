package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.ChatMessageDto;
import com.truehunt.social.security.AuthenticatedUserResolver;
import com.truehunt.social.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public MessageController(MessageService messageService, AuthenticatedUserResolver authenticatedUserResolver) {
        this.messageService = messageService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    /**
     * Handles one-to-one messages sent over the authenticated WebSocket session.
     */
    @MessageMapping("/chat.send")
    public void handleChatMessage(@Payload ChatMessageDto message, Principal principal) {
        messageService.processAndSendMessage(message, principal);
    }

    @MessageMapping("/chat.private")
    public void handlePrivateMessage(@Payload ChatMessageDto message, Principal principal) {
        messageService.processAndSendMessage(message, principal);
    }

    /**
     * REST endpoint for sending a message to the authenticated recipient.
     */
    @PostMapping("/api/messages")
    @ResponseBody
    public ResponseEntity<ApiResponse<ChatMessageDto>> sendRestMessage(
            @RequestBody ChatMessageDto message,
            HttpServletRequest request
    ) {
        Principal callerPrincipal = () -> authenticatedUserResolver.requireEmail(request);
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
            HttpServletRequest request
    ) {
        String caller = authenticatedUserResolver.requireEmail(request);
        List<Map<String, Object>> messages = messageService.getMessagesForConversation(conversationId, caller);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
}
