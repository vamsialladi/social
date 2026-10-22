package com.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.dto.request.CreateConversationRequest;
import com.social.dto.request.SendMessageRequest;
import com.social.entity.Attachment;
import com.social.entity.Conversation;
import com.social.entity.Message;
import com.social.repository.ConversationRepository;
import com.social.repository.MessageRepository;
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
class ConversationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    private static final String USER_ALICE = "user-alice-123";
    private static final String USER_BOB = "user-bob-456";
    private static final String USER_CHARLIE = "user-charlie-789";

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create or retrieve a conversation between two users")
    void testCreateConversation() throws Exception {
        CreateConversationRequest request = CreateConversationRequest.builder()
                .participantId(USER_BOB)
                .build();

        mockMvc.perform(post("/api/conversations")
                        .header("X-User-Id", USER_ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data._id").isNotEmpty())
                .andExpect(jsonPath("$.data.participantIds", hasItems(USER_ALICE, USER_BOB)))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    @DisplayName("Should list signed-in user's conversations only")
    void testListConversations() throws Exception {
        Conversation conv1 = Conversation.builder()
                .id("conv-1")
                .participantIds(List.of(USER_ALICE, USER_BOB))
                .createdAt(Instant.now().minusSeconds(100))
                .lastMessageAt(Instant.now().minusSeconds(50))
                .build();
        conversationRepository.save(conv1);

        Conversation conv2 = Conversation.builder()
                .id("conv-2")
                .participantIds(List.of(USER_BOB, USER_CHARLIE))
                .createdAt(Instant.now().minusSeconds(80))
                .lastMessageAt(Instant.now().minusSeconds(40))
                .build();
        conversationRepository.save(conv2);

        mockMvc.perform(get("/api/conversations")
                        .header("X-User-Id", USER_ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value("conv-1"))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    @DisplayName("Should enforce membership check before reading messages (403 for non-participants)")
    void testMembershipCheckOnReadMessages() throws Exception {
        Conversation conv = Conversation.builder()
                .id("conv-secret")
                .participantIds(List.of(USER_ALICE, USER_BOB))
                .createdAt(Instant.now())
                .lastMessageAt(Instant.now())
                .build();
        conversationRepository.save(conv);

        mockMvc.perform(get("/api/conversations/conv-secret/messages")
                        .header("X-User-Id", USER_CHARLIE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message", containsString("not a participant")));
    }

    @Test
    @DisplayName("Should send message, validate content, update lastMessageAt, and return message")
    void testSendMessage() throws Exception {
        Conversation conv = Conversation.builder()
                .id("conv-test-send")
                .participantIds(List.of(USER_ALICE, USER_BOB))
                .createdAt(Instant.now().minusSeconds(300))
                .lastMessageAt(Instant.now().minusSeconds(300))
                .build();
        conversationRepository.save(conv);

        SendMessageRequest msgReq = SendMessageRequest.builder()
                .body("Hello Bob, this is a test message!")
                .build();

        mockMvc.perform(post("/api/conversations/conv-test-send/messages")
                        .header("X-User-Id", USER_ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data._id").isNotEmpty())
                .andExpect(jsonPath("$.data.conversationId").value("conv-test-send"))
                .andExpect(jsonPath("$.data.senderId").value(USER_ALICE))
                .andExpect(jsonPath("$.data.body").value("Hello Bob, this is a test message!"))
                .andExpect(jsonPath("$.data.readAt").doesNotExist());

        Conversation updatedConv = conversationRepository.findById("conv-test-send").orElseThrow();
        assertTrue(updatedConv.getLastMessageAt().isAfter(conv.getCreatedAt()));
    }

    @Test
    @DisplayName("Should allow message with attachment and empty body")
    void testSendMessageWithAttachment() throws Exception {
        Conversation conv = Conversation.builder()
                .id("conv-attachment")
                .participantIds(List.of(USER_ALICE, USER_BOB))
                .createdAt(Instant.now())
                .lastMessageAt(Instant.now())
                .build();
        conversationRepository.save(conv);

        Attachment attachment = Attachment.builder()
                .id("att-1")
                .url("https://cdn.social.com/images/photo.jpg")
                .type("image/jpeg")
                .name("photo.jpg")
                .size(1024L)
                .build();

        SendMessageRequest msgReq = SendMessageRequest.builder()
                .body(null)
                .attachments(List.of(attachment))
                .build();

        mockMvc.perform(post("/api/conversations/conv-attachment/messages")
                        .header("X-User-Id", USER_ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.attachments", hasSize(1)))
                .andExpect(jsonPath("$.data.attachments[0].url").value("https://cdn.social.com/images/photo.jpg"));
    }

    @Test
    @DisplayName("Should reject message when both body and attachments are empty (400 Bad Request)")
    void testSendMessageEmptyBodyAndNoAttachments() throws Exception {
        Conversation conv = Conversation.builder()
                .id("conv-empty-msg")
                .participantIds(List.of(USER_ALICE, USER_BOB))
                .createdAt(Instant.now())
                .lastMessageAt(Instant.now())
                .build();
        conversationRepository.save(conv);

        SendMessageRequest msgReq = SendMessageRequest.builder()
                .body("")
                .attachments(List.of())
                .build();

        mockMvc.perform(post("/api/conversations/conv-empty-msg/messages")
                        .header("X-User-Id", USER_ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.message", containsString("Message body is required")));
    }

    @Test
    @DisplayName("Should support cursor-based pagination for messages")
    void testMessagesCursorPagination() throws Exception {
        Conversation conv = Conversation.builder()
                .id("conv-pagination")
                .participantIds(List.of(USER_ALICE, USER_BOB))
                .createdAt(Instant.now().minusSeconds(1000))
                .lastMessageAt(Instant.now())
                .build();
        conversationRepository.save(conv);

        Instant baseTime = Instant.now().minusSeconds(500);
        for (int i = 1; i <= 5; i++) {
            Message msg = Message.builder()
                    .id("msg-" + i)
                    .conversationId("conv-pagination")
                    .senderId(USER_ALICE)
                    .body("Message " + i)
                    .createdAt(baseTime.plusSeconds(i * 10))
                    .build();
            messageRepository.save(msg);
        }

        mockMvc.perform(get("/api/conversations/conv-pagination/messages?limit=2")
                        .header("X-User-Id", USER_BOB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.meta.nextCursor").isNotEmpty());
    }

    @Test
    @DisplayName("Should mark messages as read and update readAt timestamp")
    void testMarkAsRead() throws Exception {
        Conversation conv = Conversation.builder()
                .id("conv-read-test")
                .participantIds(List.of(USER_ALICE, USER_BOB))
                .createdAt(Instant.now())
                .lastMessageAt(Instant.now())
                .build();
        conversationRepository.save(conv);

        Message msg = Message.builder()
                .id("msg-unread-1")
                .conversationId("conv-read-test")
                .senderId(USER_ALICE)
                .body("Unread message")
                .readAt(null)
                .createdAt(Instant.now())
                .build();
        messageRepository.save(msg);

        mockMvc.perform(patch("/api/conversations/conv-read-test/read")
                        .header("X-User-Id", USER_BOB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markedReadCount").value(1));

        Message readMsg = messageRepository.findById("msg-unread-1").orElseThrow();
        assertNotNull(readMsg.getReadAt());
    }

    @Test
    @DisplayName("Edge Case: Should return 401 Unauthorized when auth header is missing")
    void testMissingAuthReturns401() throws Exception {
        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("NOT_AUTHENTICATED"))
                .andExpect(jsonPath("$.error.message", containsString("authentication is missing")));
    }

    @Test
    @DisplayName("Edge Case: Should return 400 Bad Request when trying to start conversation with self")
    void testCannotStartConversationWithSelf() throws Exception {
        CreateConversationRequest request = CreateConversationRequest.builder()
                .participantId(USER_ALICE)
                .build();

        mockMvc.perform(post("/api/conversations")
                        .header("X-User-Id", USER_ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.message", containsString("at least two participants")));
    }

    @Test
    @DisplayName("Edge Case: Should return existing conversation when creating duplicate 1-to-1 conversation (Idempotent)")
    void testIdempotentConversationCreation() throws Exception {
        CreateConversationRequest request = CreateConversationRequest.builder()
                .participantId(USER_BOB)
                .build();

        String res1 = mockMvc.perform(post("/api/conversations")
                        .header("X-User-Id", USER_ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String convId1 = objectMapper.readTree(res1).get("data").get("id").asText();

        String res2 = mockMvc.perform(post("/api/conversations")
                        .header("X-User-Id", USER_ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String convId2 = objectMapper.readTree(res2).get("data").get("id").asText();

        assertEquals(convId1, convId2, "Idempotent conversation creation must return the same conversation ID");
    }

    @Test
    @DisplayName("Edge Case: Should return 404 Not Found when fetching non-existent conversation")
    void testGetNonExistentConversationReturns404() throws Exception {
        mockMvc.perform(get("/api/conversations/non-existent-conv-id")
                        .header("X-User-Id", USER_ALICE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Edge Case: Should return 404 Not Found when sending message to non-existent conversation")
    void testSendMessageToNonExistentConversationReturns404() throws Exception {
        SendMessageRequest msgReq = SendMessageRequest.builder()
                .body("Hello?")
                .build();

        mockMvc.perform(post("/api/conversations/non-existent-conv-id/messages")
                        .header("X-User-Id", USER_ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Edge Case: Should return 403 Forbidden when non-participant tries to send a message")
    void testSendMessageForbiddenForNonParticipant() throws Exception {
        Conversation conv = Conversation.builder()
                .id("conv-private")
                .participantIds(List.of(USER_ALICE, USER_BOB))
                .createdAt(Instant.now())
                .lastMessageAt(Instant.now())
                .build();
        conversationRepository.save(conv);

        SendMessageRequest msgReq = SendMessageRequest.builder()
                .body("Intruder message")
                .build();

        mockMvc.perform(post("/api/conversations/conv-private/messages")
                        .header("X-User-Id", USER_CHARLIE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message", containsString("not a participant")));
    }

    @Test
    @DisplayName("Edge Case: Should return 400 Bad Request for invalid cursor format")
    void testInvalidCursorFormatReturns400() throws Exception {
        Conversation conv = Conversation.builder()
                .id("conv-cursor-test")
                .participantIds(List.of(USER_ALICE, USER_BOB))
                .createdAt(Instant.now())
                .lastMessageAt(Instant.now())
                .build();
        conversationRepository.save(conv);

        mockMvc.perform(get("/api/conversations/conv-cursor-test/messages?cursor=invalid-cursor-timestamp")
                        .header("X-User-Id", USER_ALICE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.message", containsString("Invalid cursor format")));
    }

    @Test
    @DisplayName("Edge Case: Should return 403 Forbidden when non-participant tries to mark messages as read")
    void testMarkAsReadForbiddenForNonParticipant() throws Exception {
        Conversation conv = Conversation.builder()
                .id("conv-read-forbidden")
                .participantIds(List.of(USER_ALICE, USER_BOB))
                .createdAt(Instant.now())
                .lastMessageAt(Instant.now())
                .build();
        conversationRepository.save(conv);

        mockMvc.perform(patch("/api/conversations/conv-read-forbidden/read")
                        .header("X-User-Id", USER_CHARLIE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Edge Case: Should support cursor pagination for conversations list")
    void testConversationsCursorPagination() throws Exception {
        Instant baseTime = Instant.now().minusSeconds(1000);
        for (int i = 1; i <= 4; i++) {
            Conversation conv = Conversation.builder()
                    .id("conv-page-" + i)
                    .participantIds(List.of(USER_ALICE, "user-other-" + i))
                    .createdAt(baseTime.plusSeconds(i * 10))
                    .lastMessageAt(baseTime.plusSeconds(i * 10))
                    .build();
            conversationRepository.save(conv);
        }

        String res = mockMvc.perform(get("/api/conversations?limit=2")
                        .header("X-User-Id", USER_ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.meta.nextCursor").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String nextCursor = objectMapper.readTree(res).get("meta").get("nextCursor").asText();

        mockMvc.perform(get("/api/conversations?cursor=" + nextCursor + "&limit=2")
                        .header("X-User-Id", USER_ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("Edge Case: Should return 404 when marking read on non-existent conversation")
    void testMarkReadNonExistentConversationReturns404() throws Exception {
        mockMvc.perform(patch("/api/conversations/non-existent-conv/read")
                        .header("X-User-Id", USER_ALICE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Edge Case: Should allow message with both body text and attachment")
    void testSendMessageWithBodyAndAttachment() throws Exception {
        Conversation conv = Conversation.builder()
                .id("conv-body-attachment")
                .participantIds(List.of(USER_ALICE, USER_BOB))
                .createdAt(Instant.now())
                .lastMessageAt(Instant.now())
                .build();
        conversationRepository.save(conv);

        Attachment attachment = Attachment.builder()
                .id("att-pdf")
                .url("https://cdn.social.com/docs/spec.pdf")
                .type("application/pdf")
                .name("spec.pdf")
                .size(2048L)
                .build();

        SendMessageRequest msgReq = SendMessageRequest.builder()
                .body("Please review this document attached.")
                .attachments(List.of(attachment))
                .build();

        mockMvc.perform(post("/api/conversations/conv-body-attachment/messages")
                        .header("X-User-Id", USER_ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.body").value("Please review this document attached."))
                .andExpect(jsonPath("$.data.attachments", hasSize(1)))
                .andExpect(jsonPath("$.data.attachments[0].url").value("https://cdn.social.com/docs/spec.pdf"));
    }
}
