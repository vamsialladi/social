package com.truehunt.social.service;

import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.dto.ChatMessageDto;
import com.truehunt.social.model.ConnectionGroupEntity;
import com.truehunt.social.model.ConversationEntity;
import com.truehunt.social.model.MessageEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ConnectionGroupRepository;
import com.truehunt.social.repository.ConversationRepository;
import com.truehunt.social.repository.MessageRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.security.Principal;
import org.springframework.messaging.simp.SimpMessagingTemplate;

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
    private ProfileRepository profileRepository;
    private ConversationService conversationService;
    private ConversationEntity conversation;
    private MessageEntity message;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ConversationRepository.class);
        messageRepository = mock(MessageRepository.class);
        messageService = mock(MessageService.class);
        userAccountRepository = mock(UserAccountRepository.class);
        profileRepository = mock(ProfileRepository.class);
        conversationService = new ConversationService(
                conversationRepository,
                messageRepository,
                messageService,
                userAccountRepository,
                profileRepository
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
        message.setReceiverId(OTHER_EMAIL);
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

        conversationService.starConversation(CONVERSATION_ID.toString(), OWNER_EMAIL);

        assertTrue(conversation.isStarred());
        assertEquals(lastActivity, conversation.getUpdatedAt());

        conversationService.starConversation(CONVERSATION_ID.toString(), OWNER_EMAIL);

        assertFalse(conversation.isStarred());
        assertEquals(lastActivity, conversation.getUpdatedAt());
    }

    @Test
    void markingConversationReadUpdatesOnlyMessagesSentToTheCurrentUser() {
        conversation.setUnread(true);
        when(messageRepository.findAllByConversationIdOrderByCreatedAtAsc(CONVERSATION_ID))
                .thenReturn(List.of(message));

        conversationService.markAsRead(CONVERSATION_ID.toString(), OTHER_EMAIL);

        assertEquals("READ", message.getStatus());
        assertTrue(message.getReadAt() != null);
        assertEquals("READ", conversation.getThread().get(0).get("status"));
        assertTrue(conversation.getThread().get(0).containsKey("readAt"));
        assertFalse(conversation.isUnread());
        verify(messageService).broadcastReadReceipt(
                org.mockito.ArgumentMatchers.eq(CONVERSATION_ID),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void openingOwnConversationDoesNotMarkOwnMessageAsRead() {
        when(messageRepository.findAllByConversationIdOrderByCreatedAtAsc(CONVERSATION_ID))
                .thenReturn(List.of(message));

        conversationService.markAsRead(CONVERSATION_ID.toString(), OWNER_EMAIL);

        assertEquals("SENT", message.getStatus());
        assertTrue(message.getReadAt() == null);
    }

    @Test
    void conversationDtoIncludesTheOtherParticipantsProfileImage() {
        ProfileEntity otherProfile = new ProfileEntity();
        otherProfile.setProfileImageUrl("https://example.com/other.png");
        when(profileRepository.findByUserId(OTHER_EMAIL)).thenReturn(Optional.of(otherProfile));

        Map<String, Object> result = conversationService.getConversation(
                CONVERSATION_ID.toString(),
                OWNER_EMAIL
        );

        assertEquals("https://example.com/other.png", result.get("profileImageUrl"));
        assertEquals("https://example.com/other.png", result.get("avatar"));
    }

    @Test
    void groupConversationIsCreatedForTheOwnerAndItsMembers() {
        ConnectionGroupRepository groupRepository = mock(ConnectionGroupRepository.class);
        ConversationService service = new ConversationService(
                conversationRepository,
                messageRepository,
                messageService,
                userAccountRepository,
                profileRepository,
                null,
                groupRepository);
        UserAccountEntity owner = account(OWNER_EMAIL, "owner-id");
        UserAccountEntity other = account(OTHER_EMAIL, "other-id");
        when(userAccountRepository.findByPublicUserId("owner-id")).thenReturn(Optional.of(owner));
        when(userAccountRepository.findByPublicUserId("other-id")).thenReturn(Optional.of(other));
        when(conversationRepository.findFirstByConnectionGroupId(42L)).thenReturn(Optional.empty());

        ConnectionGroupEntity group = new ConnectionGroupEntity();
        group.setId(42L);
        group.setOwnerId("owner-id");
        group.setName("Production Crew");
        group.setImageUrl("https://example.com/group.png");
        group.setMemberProfileIds(List.of("other-id"));

        Map<String, Object> result = service.getOrCreateGroupConversation(group, OWNER_EMAIL);

        assertEquals("Production Crew", result.get("name"));
        assertEquals("https://example.com/group.png", result.get("profileImageUrl"));
        assertEquals(true, result.get("isGroupConversation"));
        assertEquals(List.of(OWNER_EMAIL, OTHER_EMAIL), result.get("groupParticipantIds"));
        verify(conversationRepository).save(any(ConversationEntity.class));
    }

    @Test
    void groupMessagesArePersistedOnceAndDeliveredToEveryParticipant() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        MessageService service = new MessageService(
                messageRepository,
                conversationRepository,
                userAccountRepository,
                profileRepository,
                messagingTemplate);
        ConversationEntity groupConversation = new ConversationEntity();
        groupConversation.setId(42L);
        groupConversation.setConnectionGroupId(7L);
        groupConversation.setName("Production Crew");
        groupConversation.setGroupParticipantIds(List.of(OWNER_EMAIL, OTHER_EMAIL));
        when(conversationRepository.findById(42L)).thenReturn(Optional.of(groupConversation));
        when(userAccountRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(account(OWNER_EMAIL, "owner-id")));
        when(userAccountRepository.findByEmail(OTHER_EMAIL)).thenReturn(Optional.of(account(OTHER_EMAIL, "other-id")));
        when(messageRepository.save(any(MessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.save(any(ConversationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessageDto request = new ChatMessageDto();
        request.setConversationId(42L);
        request.setContent("Hello, team");
        service.processAndSendMessage(request, (Principal) () -> OWNER_EMAIL);

        assertEquals("Hello, team", groupConversation.getThread().get(0).get("content"));
        assertEquals("group:7", groupConversation.getThread().get(0).get("receiverId"));
        verify(messageRepository).save(any(MessageEntity.class));
        verify(messagingTemplate).convertAndSendToUser(
                org.mockito.ArgumentMatchers.eq(OWNER_EMAIL),
                org.mockito.ArgumentMatchers.eq("/queue/messages"),
                any(ChatMessageDto.class));
        verify(messagingTemplate).convertAndSendToUser(
                org.mockito.ArgumentMatchers.eq(OTHER_EMAIL),
                org.mockito.ArgumentMatchers.eq("/queue/messages"),
                any(ChatMessageDto.class));
    }

    @Test
    void groupConversationDtoKeepsTheGroupNameAndTracksReadStatePerMember() {
        ConversationEntity groupConversation = new ConversationEntity();
        groupConversation.setConnectionGroupId(42L);
        groupConversation.setName("Production Crew");
        groupConversation.setGroupParticipantIds(List.of(OWNER_EMAIL, OTHER_EMAIL));
        String sentAt = LocalDateTime.now().toString();
        Map<String, Object> groupMessage = new HashMap<>();
        groupMessage.put("id", "group-message");
        groupMessage.put("senderId", OWNER_EMAIL);
        groupMessage.put("sentAt", sentAt);
        groupMessage.put("content", "Call time is 9 AM");
        groupConversation.setThread(List.of(groupMessage));

        Map<String, Object> unreadDto = groupConversation.toDto(OTHER_EMAIL, java.util.Set.of(OTHER_EMAIL));
        assertEquals("Production Crew", unreadDto.get("name"));
        assertEquals(true, unreadDto.get("isGroupConversation"));
        assertEquals(true, unreadDto.get("unread"));

        groupConversation.setGroupReadAt(Map.of(OTHER_EMAIL, sentAt));
        Map<String, Object> readDto = groupConversation.toDto(OTHER_EMAIL, java.util.Set.of(OTHER_EMAIL));
        assertEquals(false, readDto.get("unread"));
    }

    @Test
    void groupConversationDtoIncludesGroupImageAndUpdatesProperly() {
        ConversationEntity groupConversation = new ConversationEntity();
        groupConversation.setId(99L);
        groupConversation.setConnectionGroupId(42L);
        groupConversation.setName("Production Crew");
        groupConversation.setGroupImageUrl("/uploads/group-avatar.png");
        groupConversation.setGroupParticipantIds(List.of(OWNER_EMAIL, OTHER_EMAIL));

        Map<String, Object> dto = groupConversation.toDto(OTHER_EMAIL, java.util.Set.of(OTHER_EMAIL));
        assertEquals("/uploads/group-avatar.png", dto.get("groupImageUrl"));
        assertEquals("/uploads/group-avatar.png", dto.get("avatar"));
        assertEquals("/uploads/group-avatar.png", dto.get("profileImageUrl"));
    }

    private static UserAccountEntity account(String email, String publicUserId) {
        UserAccountEntity account = new UserAccountEntity();
        account.setEmail(email);
        account.setPublicUserId(publicUserId);
        return account;
    }
}
