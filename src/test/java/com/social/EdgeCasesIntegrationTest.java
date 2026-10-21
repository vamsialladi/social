package com.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.config.UserContextInterceptor;
import com.social.dto.request.CreateOpportunityRequest;
import com.social.dto.request.CreatePostRequest;
import com.social.dto.request.CreateUserRequest;
import com.social.dto.request.SendConnectionRequest;
import com.social.dto.request.UpdateConnectionStatusRequest;
import com.social.dto.response.UserProfileResponse;
import com.social.entity.ConnectionStatus;
import com.social.repository.ConnectionRepository;
import com.social.repository.NotificationRepository;
import com.social.repository.PostRepository;
import com.social.repository.UserRepository;
import com.social.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EdgeCasesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private UserProfileResponse user1;
    private UserProfileResponse user2;
    private UserProfileResponse user3;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        connectionRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        user1 = userService.createUser(CreateUserRequest.builder()
                .username("edge_user_1")
                .name("User One")
                .email("user1@edge.com")
                .role("ARTIST")
                .build());

        user2 = userService.createUser(CreateUserRequest.builder()
                .username("edge_user_2")
                .name("User Two")
                .email("user2@edge.com")
                .role("EMPLOYER")
                .build());

        user3 = userService.createUser(CreateUserRequest.builder()
                .username("edge_user_3")
                .name("User Three")
                .email("user3@edge.com")
                .role("DIRECTOR")
                .build());
    }

    // ==========================================
    // MODULE 5: OPPORTUNITIES EDGE CASES
    // ==========================================

    @Test
    @DisplayName("Edge Case: Expiry date in the past must be rejected")
    void testPastExpiryDateRejected() throws Exception {
        CreateOpportunityRequest request = CreateOpportunityRequest.builder()
                .title("Past Opportunity")
                .content("Valid description")
                .expiryDate(Instant.now().minus(2, ChronoUnit.DAYS))
                .build();

        mockMvc.perform(post("/api/opportunities")
                        .header(UserContextInterceptor.USER_ID_HEADER, user1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Expiry date cannot be in the past")));
    }

    @Test
    @DisplayName("Edge Case: Non-existent author in X-User-Id header returns 404")
    void testNonExistentAuthorReturns404() throws Exception {
        CreateOpportunityRequest request = CreateOpportunityRequest.builder()
                .title("Valid Title")
                .content("Valid Content")
                .build();

        mockMvc.perform(post("/api/opportunities")
                        .header(UserContextInterceptor.USER_ID_HEADER, "non_existent_user_id_12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Author not found")));
    }

    @Test
    @DisplayName("Edge Case: Invalid cursor format returns 400 Bad Request")
    void testInvalidCursorFormatReturns400() throws Exception {
        mockMvc.perform(get("/api/opportunities")
                        .param("cursor", "completely_invalid_timestamp_cursor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid cursor format")));
    }

    @Test
    @DisplayName("Edge Case: Fetching non-existent opportunity ID returns 404")
    void testGetNonExistentOpportunityReturns404() throws Exception {
        mockMvc.perform(get("/api/opportunities/non_existent_opp_id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Opportunity not found")));
    }

    @Test
    @DisplayName("Edge Case: Querying ordinary post via GET /api/opportunities/:id returns 404")
    void testQueryingOrdinaryPostAsOpportunityReturns404() throws Exception {
        // Create an ordinary post
        CreatePostRequest postRequest = CreatePostRequest.builder()
                .content("Just an ordinary post")
                .build();

        MvcResult result = mockMvc.perform(post("/api/posts")
                        .header(UserContextInterceptor.USER_ID_HEADER, user1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String postId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // Attempt to fetch ordinary post via opportunities endpoint
        mockMvc.perform(get("/api/opportunities/" + postId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not an opportunity")));
    }

    @Test
    @DisplayName("Edge Case: Cursor pagination advances through pages accurately")
    void testCursorPaginationAdvancement() throws Exception {
        // Create 3 opportunities with slight time differences
        for (int i = 1; i <= 3; i++) {
            CreateOpportunityRequest opp = CreateOpportunityRequest.builder()
                    .title("Opportunity #" + i)
                    .content("Description #" + i)
                    .build();

            mockMvc.perform(post("/api/opportunities")
                            .header(UserContextInterceptor.USER_ID_HEADER, user2.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(opp)))
                    .andExpect(status().isCreated());
            Thread.sleep(10);
        }

        // Fetch Page 1 with limit=2 -> hasMore=true, nextCursor != null
        MvcResult page1 = mockMvc.perform(get("/api/opportunities")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.hasMore", is(true)))
                .andExpect(jsonPath("$.nextCursor", notNullValue()))
                .andReturn();

        String nextCursor = objectMapper.readTree(page1.getResponse().getContentAsString())
                .get("nextCursor").asText();

        // Fetch Page 2 using nextCursor -> hasMore=false, 1 remaining item
        mockMvc.perform(get("/api/opportunities")
                        .param("cursor", nextCursor)
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.hasMore", is(false)));
    }

    // ==========================================
    // MODULE 6: NETWORK & CONNECTIONS EDGE CASES
    // ==========================================

    @Test
    @DisplayName("Edge Case: Missing or empty recipientId returns 400 Bad Request")
    void testMissingRecipientIdReturns400() throws Exception {
        SendConnectionRequest request = SendConnectionRequest.builder()
                .recipientId("")
                .build();

        mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, user1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.recipientId", notNullValue()));
    }

    @Test
    @DisplayName("Edge Case: Non-existent recipient returns 404 Not Found")
    void testNonExistentRecipientReturns404() throws Exception {
        SendConnectionRequest request = SendConnectionRequest.builder()
                .recipientId("ghost_user_99999")
                .build();

        mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, user1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Target user not found")));
    }

    @Test
    @DisplayName("Edge Case: Reverse duplicate connection request returns 409 Conflict")
    void testReverseDuplicateConnectionReturns409() throws Exception {
        // User1 -> User2 pending request
        mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, user1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SendConnectionRequest.builder().recipientId(user2.getId()).build())))
                .andExpect(status().isCreated());

        // User2 attempts to send request to User1 -> 409 Conflict
        mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, user2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SendConnectionRequest.builder().recipientId(user1.getId()).build())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("Edge Case: Connection request when already ACCEPTED returns 409 Conflict")
    void testRequestWhenAlreadyAcceptedReturns409() throws Exception {
        // Create request User1 -> User2
        MvcResult res = mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, user1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SendConnectionRequest.builder().recipientId(user2.getId()).build())))
                .andExpect(status().isCreated())
                .andReturn();

        String connId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // User2 accepts
        mockMvc.perform(patch("/api/connections/" + connId)
                        .header(UserContextInterceptor.USER_ID_HEADER, user2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateConnectionStatusRequest.builder().status(ConnectionStatus.ACCEPTED).build())))
                .andExpect(status().isOk());

        // Either User1 or User2 attempts to connect again
        mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, user1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SendConnectionRequest.builder().recipientId(user2.getId()).build())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already connected")));
    }

    @Test
    @DisplayName("Edge Case: Changing connection status back to PENDING returns 400 Bad Request")
    void testRevertingStatusToPendingReturns400() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, user1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SendConnectionRequest.builder().recipientId(user2.getId()).build())))
                .andExpect(status().isCreated())
                .andReturn();

        String connId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // Attempt to update status to pending
        mockMvc.perform(patch("/api/connections/" + connId)
                        .header(UserContextInterceptor.USER_ID_HEADER, user2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateConnectionStatusRequest.builder().status(ConnectionStatus.PENDING).build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot change status back to pending")));
    }

    @Test
    @DisplayName("Edge Case: Non-participant cannot view private connection details (403 Forbidden)")
    void testNonParticipantCannotViewConnection() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, user1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SendConnectionRequest.builder().recipientId(user2.getId()).build())))
                .andExpect(status().isCreated())
                .andReturn();

        String connId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // User3 (stranger) attempts to view connection between User1 and User2
        mockMvc.perform(get("/api/connections/" + connId)
                        .header(UserContextInterceptor.USER_ID_HEADER, user3.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("permission to view this connection")));
    }

    @Test
    @DisplayName("Edge Case: Blocked user cannot contact blocker and is excluded from suggestions")
    void testBlockedUserForbiddenAndExcluded() throws Exception {
        // User1 sends request to User2
        MvcResult res = mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, user1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SendConnectionRequest.builder().recipientId(user2.getId()).build())))
                .andExpect(status().isCreated())
                .andReturn();

        String connId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // User2 blocks User1
        mockMvc.perform(patch("/api/connections/" + connId)
                        .header(UserContextInterceptor.USER_ID_HEADER, user2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateConnectionStatusRequest.builder().status(ConnectionStatus.BLOCKED).build())))
                .andExpect(status().isOk());

        // User1 attempts to contact blocker User2 -> 403 Forbidden
        mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, user1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SendConnectionRequest.builder().recipientId(user2.getId()).build())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Cannot send connection request to this user")));

        // Blocked user is excluded from suggestions for User1
        mockMvc.perform(get("/api/connections/suggestions")
                        .header(UserContextInterceptor.USER_ID_HEADER, user1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", not(hasItem(user2.getId()))));
    }

    @Test
    @DisplayName("Edge Case: Re-requesting after REJECTED re-activates request cleanly to PENDING")
    void testReRequestingAfterRejectionReactivates() throws Exception {
        // User1 sends request to User2
        MvcResult res = mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, user1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SendConnectionRequest.builder().recipientId(user2.getId()).build())))
                .andExpect(status().isCreated())
                .andReturn();

        String connId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // User2 rejects
        mockMvc.perform(patch("/api/connections/" + connId)
                        .header(UserContextInterceptor.USER_ID_HEADER, user2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateConnectionStatusRequest.builder().status(ConnectionStatus.REJECTED).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("rejected")));

        // User1 re-sends request -> cleanly re-activates to pending
        mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, user1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SendConnectionRequest.builder().recipientId(user2.getId()).build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("pending")));
    }
}
