package com.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.dto.request.MarkNotificationsReadRequest;
import com.social.dto.request.SendMessageRequest;
import com.social.entity.Conversation;
import com.social.entity.Notification;
import com.social.entity.NotificationType;
import com.social.repository.ConversationRepository;
import com.social.repository.MessageRepository;
import com.social.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    private static final String USER_ALICE = "user-alice-123";
    private static final String USER_BOB = "user-bob-456";
    private static final String USER_CHARLIE = "user-charlie-789";

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    @Test
    @DisplayName("Should list only notifications belonging to the signed-in user")
    void testListNotificationsOnlyForSignedInUser() throws Exception {
        Notification notifAlice = Notification.builder()
                .id("notif-1")
                .recipientId(USER_ALICE)
                .actorId(USER_BOB)
                .type(NotificationType.CONNECTION_REQUEST)
                .resourceType("connection")
                .resourceId("conn-100")
                .createdAt(Instant.now().minusSeconds(60))
                .build();
        notificationRepository.save(notifAlice);

        Notification notifBob = Notification.builder()
                .id("notif-2")
                .recipientId(USER_BOB)
                .actorId(USER_CHARLIE)
                .type(NotificationType.MESSAGE)
                .resourceType("conversation")
                .resourceId("conv-200")
                .createdAt(Instant.now().minusSeconds(30))
                .build();
        notificationRepository.save(notifBob);

        mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", USER_ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value("notif-1"))
                .andExpect(jsonPath("$.data[0]._id").value("notif-1"))
                .andExpect(jsonPath("$.data[0].type").value("connection_request"))
                .andExpect(jsonPath("$.meta").exists());

        mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", USER_BOB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value("notif-2"))
                .andExpect(jsonPath("$.data[0].type").value("message"));
    }

    @Test
    @DisplayName("Should support cursor-based pagination for notifications")
    void testCursorPagination() throws Exception {
        Instant baseTime = Instant.now().minusSeconds(1000);
        for (int i = 1; i <= 5; i++) {
            Notification notif = Notification.builder()
                    .id("notif-page-" + i)
                    .recipientId(USER_ALICE)
                    .actorId(USER_BOB)
                    .type(NotificationType.CONNECTION_ACCEPTED)
                    .resourceType("connection")
                    .resourceId("conn-" + i)
                    .createdAt(baseTime.plusSeconds(i * 10))
                    .build();
            notificationRepository.save(notif);
        }

        String responseContent = mockMvc.perform(get("/api/notifications?limit=2")
                        .header("X-User-Id", USER_ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.meta.nextCursor").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String nextCursor = objectMapper.readTree(responseContent).get("meta").get("nextCursor").asText();

        mockMvc.perform(get("/api/notifications?cursor=" + nextCursor + "&limit=2")
                        .header("X-User-Id", USER_ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("Should mark a single notification as read after ownership verification")
    void testMarkOneAsRead() throws Exception {
        Notification notif = Notification.builder()
                .id("notif-mark-read")
                .recipientId(USER_ALICE)
                .actorId(USER_BOB)
                .type(NotificationType.MESSAGE)
                .resourceType("conversation")
                .resourceId("conv-300")
                .readAt(null)
                .createdAt(Instant.now())
                .build();
        notificationRepository.save(notif);

        mockMvc.perform(patch("/api/notifications/notif-mark-read/read")
                        .header("X-User-Id", USER_ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("notif-mark-read"))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        Notification updated = notificationRepository.findById("notif-mark-read").orElseThrow();
        assertNotNull(updated.getReadAt());
    }

    @Test
    @DisplayName("Should deny marking as read if user is not the recipient (403 Forbidden)")
    void testMarkOneAsReadForbidden() throws Exception {
        Notification notif = Notification.builder()
                .id("notif-alice-only")
                .recipientId(USER_ALICE)
                .actorId(USER_BOB)
                .type(NotificationType.MESSAGE)
                .resourceType("conversation")
                .resourceId("conv-400")
                .createdAt(Instant.now())
                .build();
        notificationRepository.save(notif);

        mockMvc.perform(patch("/api/notifications/notif-alice-only/read")
                        .header("X-User-Id", USER_BOB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message", containsString("permission")));
    }

    @Test
    @DisplayName("Should mark many notifications as read with specific IDs")
    void testMarkManyAsReadWithIds() throws Exception {
        Notification n1 = Notification.builder()
                .id("notif-batch-1")
                .recipientId(USER_ALICE)
                .actorId(USER_BOB)
                .type(NotificationType.MESSAGE)
                .resourceType("conversation")
                .resourceId("conv-501")
                .createdAt(Instant.now())
                .build();
        Notification n2 = Notification.builder()
                .id("notif-batch-2")
                .recipientId(USER_ALICE)
                .actorId(USER_BOB)
                .type(NotificationType.MESSAGE)
                .resourceType("conversation")
                .resourceId("conv-502")
                .createdAt(Instant.now())
                .build();
        Notification n3 = Notification.builder()
                .id("notif-batch-3")
                .recipientId(USER_ALICE)
                .actorId(USER_BOB)
                .type(NotificationType.MESSAGE)
                .resourceType("conversation")
                .resourceId("conv-503")
                .createdAt(Instant.now())
                .build();

        notificationRepository.saveAll(List.of(n1, n2, n3));

        MarkNotificationsReadRequest req = new MarkNotificationsReadRequest(List.of("notif-batch-1", "notif-batch-2"));

        mockMvc.perform(patch("/api/notifications/read")
                        .header("X-User-Id", USER_ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markedReadCount").value(2));

        assertTrue(notificationRepository.findById("notif-batch-1").orElseThrow().getReadAt() != null);
        assertTrue(notificationRepository.findById("notif-batch-2").orElseThrow().getReadAt() != null);
        assertNull(notificationRepository.findById("notif-batch-3").orElseThrow().getReadAt());
    }

    @Test
    @DisplayName("Should mark all unread notifications as read when no IDs are provided")
    void testMarkAllAsRead() throws Exception {
        Notification n1 = Notification.builder()
                .id("notif-all-1")
                .recipientId(USER_ALICE)
                .actorId(USER_BOB)
                .type(NotificationType.CONNECTION_REQUEST)
                .resourceType("connection")
                .resourceId("conn-601")
                .createdAt(Instant.now())
                .build();
        Notification n2 = Notification.builder()
                .id("notif-all-2")
                .recipientId(USER_ALICE)
                .actorId(USER_CHARLIE)
                .type(NotificationType.MESSAGE)
                .resourceType("conversation")
                .resourceId("conv-602")
                .createdAt(Instant.now())
                .build();

        notificationRepository.saveAll(List.of(n1, n2));

        mockMvc.perform(patch("/api/notifications/read")
                        .header("X-User-Id", USER_ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markedReadCount").value(2));

        assertEquals(0, notificationRepository.countByRecipientIdAndReadAtIsNull(USER_ALICE));
    }

    @Test
    @DisplayName("Should create recipient notification when message is sent (Module 7 & 8 integration)")
    void testMessageSendTriggersNotification() throws Exception {
        Conversation conv = Conversation.builder()
                .id("conv-notif-integration")
                .participantIds(List.of(USER_ALICE, USER_BOB))
                .createdAt(Instant.now())
                .lastMessageAt(Instant.now())
                .build();
        conversationRepository.save(conv);

        SendMessageRequest msgReq = SendMessageRequest.builder()
                .body("Hello Bob, testing notification trigger!")
                .build();

        mockMvc.perform(post("/api/conversations/conv-notif-integration/messages")
                        .header("X-User-Id", USER_ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated());

        Thread.sleep(200);

        mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", USER_BOB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].recipientId").value(USER_BOB))
                .andExpect(jsonPath("$.data[0].actorId").value(USER_ALICE))
                .andExpect(jsonPath("$.data[0].type").value("message"))
                .andExpect(jsonPath("$.data[0].resourceType").value("conversation"))
                .andExpect(jsonPath("$.data[0].resourceId").value("conv-notif-integration"))
                .andExpect(jsonPath("$.data[0].readAt").doesNotExist());
    }

    @Test
    @DisplayName("Edge Case: Should return 401 Unauthorized when auth header is missing on notifications endpoint")
    void testMissingAuthHeaderReturns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("NOT_AUTHENTICATED"))
                .andExpect(jsonPath("$.error.message", containsString("authentication is missing")));
    }

    @Test
    @DisplayName("Edge Case: Should return 404 Not Found when marking non-existent notification as read")
    void testMarkNonExistentNotificationReadReturns404() throws Exception {
        mockMvc.perform(patch("/api/notifications/non-existent-notif-id/read")
                        .header("X-User-Id", USER_ALICE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Edge Case: Should return 400 Bad Request for invalid cursor format on notifications")
    void testInvalidCursorFormatReturns400() throws Exception {
        mockMvc.perform(get("/api/notifications?cursor=invalid-cursor-value")
                        .header("X-User-Id", USER_ALICE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.message", containsString("Invalid cursor format")));
    }

    @Test
    @DisplayName("Edge Case: Should return empty page with unreadCount = 0 when user has no notifications")
    void testEmptyNotificationsReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", "user-with-zero-notifs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.meta.nextCursor").doesNotExist());
    }

    @Test
    @DisplayName("Edge Case: Sender must never receive a notification for their own message (no self-notification)")
    void testNoSelfNotification() throws Exception {
        Conversation conv = Conversation.builder()
                .id("conv-no-self-notif")
                .participantIds(List.of(USER_ALICE, USER_BOB))
                .createdAt(Instant.now())
                .lastMessageAt(Instant.now())
                .build();
        conversationRepository.save(conv);

        SendMessageRequest msgReq = SendMessageRequest.builder()
                .body("Alice says hi")
                .build();

        mockMvc.perform(post("/api/conversations/conv-no-self-notif/messages")
                        .header("X-User-Id", USER_ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated());

        Thread.sleep(200);

        mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", USER_ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("Edge Case: Should return 401 Unauthorized when marking notifications as read without auth")
    void testMarkManyAsReadMissingAuthReturns401() throws Exception {
        mockMvc.perform(patch("/api/notifications/read"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("NOT_AUTHENTICATED"));
    }

    @Test
    @DisplayName("Edge Case: Batch mark as read should only affect the authenticated user's notifications")
    void testBatchMarkReadDoesNotAffectOtherUsers() throws Exception {
        Notification bobNotif = Notification.builder()
                .id("notif-bob-batch")
                .recipientId(USER_BOB)
                .actorId(USER_CHARLIE)
                .type(NotificationType.MESSAGE)
                .resourceType("conversation")
                .resourceId("conv-bob")
                .createdAt(Instant.now())
                .build();
        notificationRepository.save(bobNotif);

        // Alice tries to mark Bob's notification as read via batch read
        MarkNotificationsReadRequest req = new MarkNotificationsReadRequest(List.of("notif-bob-batch"));

        mockMvc.perform(patch("/api/notifications/read")
                        .header("X-User-Id", USER_ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markedReadCount").value(0));

        // Bob's notification must still be unread
        Notification notif = notificationRepository.findById("notif-bob-batch").orElseThrow();
        assertNull(notif.getReadAt());
    }
}
