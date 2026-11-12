package com.truehunt.social.service;

import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.ConversationEntity;
import com.truehunt.social.model.MessageEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ConversationRepository;
import com.truehunt.social.repository.MessageRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationServiceMessageActionsTest {

    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String OTHER_EMAIL = "other@example.com";
    private static final Long CONVERSATION_ID = 12L;
    private static final Long MESSAGE_ID = 7L;

    private ConversationRepository conversationRepository;
    private MessageRepository messageRepository;
    private MessageService messageService;
    private UserAccountRepository userAccountRepository;
    private ConversationService conversationService;
    private ConversationEntity conversation;
    private MessageEntity message;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ConversationRepository.class);
        messageRepository = mock(MessageRepository.class);
        messageService = mock(MessageService.class);
        userAccountRepository = mock(UserAccountRepository.class);
        conversationService = new ConversationService(
                conversationRepository,
                messageRepository,
                messageService,
                userAccountRepository
        );

        UserAccountEntity owner = account(OWNER_EMAIL, "owner-id");
        UserAccountEntity other = account(OTHER_EMAIL, "other-id");
        when(userAccountRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(userAccountRepository.findByEmail(OTHER_EMAIL)).thenReturn(Optional.of(other));

        conversation = new ConversationEntity();
        conversation.setId(CONVERSATION_ID);
        conversation.setUser1Id(OWNER_EMAIL);
        conversation.setUser2Id(OTHER_EMAIL);
        conversation.setUser1Name("Owner");
        conversation.setUser2Name("Other");
        conversation.setName("Other");

        Map<String, Object> threadMessage = new HashMap<>();
        threadMessage.put("id", "msg-7");
        threadMessage.put("messageId", MESSAGE_ID);
        threadMessage.put("senderId", OWNER_EMAIL);
        threadMessage.put("content", "Original message");
        threadMessage.put("direction", "outgoing");
        conversation.setThread(List.of(threadMessage));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(ConversationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        message = new MessageEntity();
        message.setId(MESSAGE_ID);
        message.setConversationId(CONVERSATION_ID);
        message.setSenderId(OWNER_EMAIL);
        message.setContent("Original message");
        when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(MessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void editingOwnMessageUpdatesPersistentAndConversationCopies() {
        conversationService.editMessage(CONVERSATION_ID.toString(), "msg-7", "Updated message", OWNER_EMAIL);

        assertEquals("Updated message", message.getContent());
        assertEquals("Updated message", conversation.getThread().get(0).get("content"));
        assertTrue((Boolean) conversation.getThread().get(0).get("edited"));
        verify(messageRepository).save(message);
        verify(conversationRepository).save(conversation);
    }

    @Test
    void cannotEditTheSameMessageMoreThanOnce() {
        conversationService.editMessage(CONVERSATION_ID.toString(), "msg-7", "First edit", OWNER_EMAIL);

        assertThrows(BadRequestException.class, () -> conversationService.editMessage(
                CONVERSATION_ID.toString(),
                "msg-7",
                "Second edit",
                OWNER_EMAIL
        ));
    }

    @Test
    void cannotEditAnotherParticipantsMessage() {
        assertThrows(UnauthorizedException.class, () -> conversationService.editMessage(
                CONVERSATION_ID.toString(),
                "msg-7",
                "Not allowed",
                OTHER_EMAIL
        ));
    }

    @Test
    void clearingChatDeletesMessagesAndEmptiesTheSharedThread() {
        conversationService.clearMessages(CONVERSATION_ID.toString(), OWNER_EMAIL);

        assertEquals(List.of(), conversation.getThread());
        assertEquals("Start a conversation", conversation.getPreview());
        verify(messageRepository).deleteAll(List.of());
        verify(conversationRepository).save(conversation);
    }

    @Test
    void pinningAndUnpinningPreservesConversationActivityOrder() {
        LocalDateTime lastActivity = LocalDateTime.now().minusDays(1);
        conversation.setUpdatedAt(lastActivity);

        conversationService.starConversation(CONVERSATION_ID.toString());

        assertTrue(conversation.isStarred());
        assertEquals(lastActivity, conversation.getUpdatedAt());

        conversationService.starConversation(CONVERSATION_ID.toString());

        assertFalse(conversation.isStarred());
        assertEquals(lastActivity, conversation.getUpdatedAt());
    }

    private static UserAccountEntity account(String email, String publicUserId) {
        UserAccountEntity account = new UserAccountEntity();
        account.setEmail(email);
        account.setPublicUserId(publicUserId);
        return account;
    }
}
