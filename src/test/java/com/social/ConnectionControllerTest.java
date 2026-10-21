package com.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.config.UserContextInterceptor;
import com.social.dto.request.CreateUserRequest;
import com.social.dto.request.SendConnectionRequest;
import com.social.dto.request.UpdateConnectionStatusRequest;
import com.social.dto.response.UserProfileResponse;
import com.social.entity.ConnectionStatus;
import com.social.repository.ConnectionRepository;
import com.social.repository.NotificationRepository;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConnectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private UserProfileResponse alice;
    private UserProfileResponse bob;
    private UserProfileResponse charlie;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        connectionRepository.deleteAll();
        userRepository.deleteAll();

        alice = userService.createUser(CreateUserRequest.builder()
                .username("alice_artist")
                .name("Alice Wonder")
                .email("alice@art.com")
                .headline("Classical Vocalist & Composer")
                .build());

        bob = userService.createUser(CreateUserRequest.builder()
                .username("bob_actor")
                .name("Bob Builder")
                .email("bob@art.com")
                .headline("Method Actor & Scriptwriter")
                .build());

        charlie = userService.createUser(CreateUserRequest.builder()
                .username("charlie_director")
                .name("Charlie Chaplin")
                .email("charlie@art.com")
                .headline("Indie Film Director")
                .build());
    }

    @Test
    @DisplayName("GET /api/connections/suggestions returns eligible profiles excluding self")
    void testGetSuggestionsExcludesSelf() throws Exception {
        mockMvc.perform(get("/api/connections/suggestions")
                        .header(UserContextInterceptor.USER_ID_HEADER, alice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].id", not(hasItem(alice.getId()))))
                .andExpect(jsonPath("$.data[*].id", hasItems(bob.getId(), charlie.getId())));
    }

    @Test
    @DisplayName("POST /api/connections sends connection request and generates notification")
    void testSendConnectionRequestSuccess() throws Exception {
        SendConnectionRequest request = SendConnectionRequest.builder()
                .recipientId(bob.getId())
                .build();

        mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, alice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.requesterId", is(alice.getId())))
                .andExpect(jsonPath("$.data.recipientId", is(bob.getId())))
                .andExpect(jsonPath("$.data.status", is("pending")))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data._id", notNullValue()));

        // Verify recipient notification created (M8)
        assertEquals(1, notificationRepository.findByRecipientIdOrderByCreatedAtDesc(bob.getId()).size());
    }

    @Test
    @DisplayName("POST /api/connections rejects self-connection")
    void testSendConnectionRequestSelfConnection() throws Exception {
        SendConnectionRequest request = SendConnectionRequest.builder()
                .recipientId(alice.getId())
                .build();

        mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, alice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("cannot send a connection request to yourself")));
    }

    @Test
    @DisplayName("POST /api/connections rejects duplicate active pair")
    void testSendConnectionRequestDuplicateActivePair() throws Exception {
        SendConnectionRequest request = SendConnectionRequest.builder()
                .recipientId(bob.getId())
                .build();

        // 1st request: Alice -> Bob
        mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, alice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 2nd request: Bob -> Alice (duplicate active pair in reverse)
        SendConnectionRequest reverseRequest = SendConnectionRequest.builder()
                .recipientId(alice.getId())
                .build();

        mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, bob.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reverseRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("PATCH /api/connections/:id allows only recipient to accept or reject")
    void testRecipientAcceptsConnection() throws Exception {
        // Alice sends connection request to Bob
        SendConnectionRequest sendReq = SendConnectionRequest.builder()
                .recipientId(bob.getId())
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, alice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String connectionId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // Charlie tries to accept Alice & Bob's request -> 403 Forbidden
        UpdateConnectionStatusRequest acceptReq = UpdateConnectionStatusRequest.builder()
                .status(ConnectionStatus.ACCEPTED)
                .build();

        mockMvc.perform(patch("/api/connections/" + connectionId)
                        .header(UserContextInterceptor.USER_ID_HEADER, charlie.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // Alice (requester) tries to accept her own request -> 403 Forbidden
        mockMvc.perform(patch("/api/connections/" + connectionId)
                        .header(UserContextInterceptor.USER_ID_HEADER, alice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // Bob (recipient) accepts -> 200 OK
        mockMvc.perform(patch("/api/connections/" + connectionId)
                        .header(UserContextInterceptor.USER_ID_HEADER, bob.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("accepted")));

        // Verify accepted connections for both Alice and Bob
        mockMvc.perform(get("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, alice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].connectedUser.id", is(bob.getId())));

        mockMvc.perform(get("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, bob.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].connectedUser.id", is(alice.getId())));

        // Alice should no longer see Bob in suggestions (already connected)
        mockMvc.perform(get("/api/connections/suggestions")
                        .header(UserContextInterceptor.USER_ID_HEADER, alice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(charlie.getId())));
    }

    @Test
    @DisplayName("Blocked users rule: blocked users cannot contact blocker and are excluded")
    void testBlockedUsersRule() throws Exception {
        // Alice sends request to Bob
        SendConnectionRequest sendReq = SendConnectionRequest.builder()
                .recipientId(bob.getId())
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, alice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String connectionId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // Bob blocks Alice
        UpdateConnectionStatusRequest blockReq = UpdateConnectionStatusRequest.builder()
                .status(ConnectionStatus.BLOCKED)
                .build();

        mockMvc.perform(patch("/api/connections/" + connectionId)
                        .header(UserContextInterceptor.USER_ID_HEADER, bob.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("blocked")));

        // Alice cannot send connection request to Bob
        mockMvc.perform(post("/api/connections")
                        .header(UserContextInterceptor.USER_ID_HEADER, alice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }
}
