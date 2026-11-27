package com.truehunt.social.controller;

import com.truehunt.social.dto.SendMessageRequest;
import com.truehunt.social.security.AuthenticatedUserResolver;
import com.truehunt.social.service.ConversationService;
import com.truehunt.social.service.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationControllerIdentityTest {

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void editMessageUsesAuthenticatedPrincipalWhenNoIdentityHeaderIsPresent() {
        ConversationService conversationService = mock(ConversationService.class);
        AuthenticatedUserResolver userResolver = mock(AuthenticatedUserResolver.class);
        ConversationController controller = new ConversationController(
                conversationService,
                mock(MessageService.class),
                userResolver
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        SendMessageRequest message = new SendMessageRequest();
        message.setContent("Updated message");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("viewer@example.com", "token", List.of())
        );
        when(userResolver.requireJwtEmail(request)).thenReturn("viewer@example.com");
        when(conversationService.editMessage("12", "34", "Updated message", "viewer@example.com"))
                .thenReturn(Map.of("id", "12"));

        controller.editMessage("12", "34", message, request);

        verify(conversationService).editMessage("12", "34", "Updated message", "viewer@example.com");
    }

    @Test
    void clearMessagesUsesAuthenticatedPrincipalWhenNoIdentityHeaderIsPresent() {
        ConversationService conversationService = mock(ConversationService.class);
        MessageService messageService = mock(MessageService.class);
        AuthenticatedUserResolver userResolver = mock(AuthenticatedUserResolver.class);
        ConversationController controller = new ConversationController(
                conversationService,
                messageService,
                userResolver
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("viewer@example.com", "token", List.of())
        );
        when(userResolver.requireJwtEmail(request)).thenReturn("viewer@example.com");
        when(conversationService.clearMessages("12", "viewer@example.com"))
                .thenReturn(Map.of("id", "12"));

        controller.clearMessages("12", request);

        verify(conversationService).clearMessages("12", "viewer@example.com");
        verify(messageService).broadcastConversationChange(12L, "CONVERSATION_CLEARED", null, null);
    }
}
