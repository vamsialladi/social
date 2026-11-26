package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.CreateConversationRequest;
import com.truehunt.social.dto.SendMessageRequest;
import com.truehunt.social.security.AuthenticatedUserResolver;
import com.truehunt.social.service.ConversationService;
import com.truehunt.social.service.MessageService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ConversationController(
            ConversationService conversationService,
            MessageService messageService,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    private String resolveCaller(HttpServletRequest request) {
        String email = authenticatedUserResolver.resolveEmail(request);
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        String headerEmail = request.getHeader("X-User-Email");
        if (headerEmail != null && !headerEmail.isBlank()) {
            return headerEmail.trim();
        }
        String headerId = request.getHeader("X-User-Id");
        if (headerId != null && !headerId.isBlank()) {
            return headerId.trim();
        }
        return authenticatedUserResolver.requireEmail(request);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConversations(
            @RequestParam(required = false) String mailbox,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(conversationService.getConversations(mailbox, resolveCaller(request))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConversation(
            @PathVariable String id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(conversationService.getConversation(id, resolveCaller(request))));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMessage(
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequest req,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(conversationService.sendMessage(id, req, resolveCaller(request))));
    }

    @PatchMapping("/{id}/messages/{messageId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> editMessage(
            @PathVariable String id,
            @PathVariable String messageId,
            @Valid @RequestBody SendMessageRequest req,
            HttpServletRequest request
    ) {
        Map<String, Object> conversation = conversationService.editMessage(
                id,
                messageId,
                req.getContent(),
                resolveCaller(request)
        );
        messageService.broadcastConversationChange(
                Long.parseLong(String.valueOf(conversation.get("id"))),
                "MESSAGE_EDITED",
                messageId,
                req.getContent().trim()
        );
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @DeleteMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<Map<String, Object>>> clearMessages(
            @PathVariable String id,
            HttpServletRequest request
    ) {
        Map<String, Object> conversation = conversationService.clearMessages(
                id,
                resolveCaller(request)
        );
        messageService.broadcastConversationChange(
                Long.parseLong(String.valueOf(conversation.get("id"))),
                "CONVERSATION_CLEARED",
                null,
                null
        );
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createConversation(
            @Valid @RequestBody CreateConversationRequest req,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(conversationService.createConversation(req, resolveCaller(request))));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAsRead(
            @PathVariable String id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(conversationService.markAsRead(id, resolveCaller(request))));
    }

    @PatchMapping("/{id}/star")
    public ResponseEntity<ApiResponse<Map<String, Object>>> starConversation(
            @PathVariable String id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(conversationService.starConversation(id, resolveCaller(request))));
    }

    @PatchMapping("/{id}/mailbox")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMailbox(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> req,
            HttpServletRequest request
    ) {
        String mailbox = (req != null && req.containsKey("mailbox")) ? req.get("mailbox") : "Focused";
        return ResponseEntity.ok(ApiResponse.success(conversationService.updateMailbox(id, mailbox, resolveCaller(request))));
    }

    @PatchMapping("/{id}/image")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateGroupImage(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request
    ) {
        String imageUrl = body != null ? (body.get("imageUrl") != null ? body.get("imageUrl") : body.get("groupImageUrl")) : null;
        return ResponseEntity.ok(ApiResponse.success(
                conversationService.updateConversationImage(id, imageUrl, resolveCaller(request))
        ));
    }

    @PutMapping("/{id}/image")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateGroupImagePut(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request
    ) {
        return updateGroupImage(id, body, request);
    }

    @PostMapping("/{id}/hide")
    public ResponseEntity<ApiResponse<Map<String, Object>>> hideConversation(
            @PathVariable String id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(conversationService.hideConversation(id, resolveCaller(request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteConversation(
            @PathVariable String id,
            HttpServletRequest request
    ) {
        conversationService.deleteConversation(id, resolveCaller(request));
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Conversation deleted successfully", "id", id)));
    }
}
