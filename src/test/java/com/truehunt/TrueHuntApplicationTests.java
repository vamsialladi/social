package com.truehunt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truehunt.dto.request.CreateOpportunityRequest;
import com.truehunt.dto.request.CreatePostRequest;
import com.truehunt.dto.request.LoginRequest;
import com.truehunt.dto.request.RegisterRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TrueHuntApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String token1;
    private static String user1Id;
    private static String token2;
    private static String user2Id;

    @Test
    @Order(1)
    public void contextLoads() {
    }

    @Test
    @Order(2)
    public void testRegisterUser1() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("artist1@truehunt.com");
        req.setPassword("secret123");
        req.setName("Artist One");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("artist1@truehunt.com"))
                .andExpect(jsonPath("$.data.profile.name").value("Artist One"))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        token1 = objectMapper.readTree(responseJson).path("data").path("token").asText();
        user1Id = objectMapper.readTree(responseJson).path("data").path("user").path("id").asText();
    }

    @Test
    @Order(3)
    public void testRegisterUser2() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("artist2@truehunt.com");
        req.setPassword("secret123");
        req.setName("Artist Two");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("artist2@truehunt.com"))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        token2 = objectMapper.readTree(responseJson).path("data").path("token").asText();
        user2Id = objectMapper.readTree(responseJson).path("data").path("user").path("id").asText();
    }

    @Test
    @Order(4)
    public void testDuplicateEmailRegistrationFails() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("artist1@truehunt.com");
        req.setPassword("secret123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    @Order(5)
    public void testLoginSuccess() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("artist1@truehunt.com");
        req.setPassword("secret123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("artist1@truehunt.com"));
    }

    @Test
    @Order(6)
    public void testGetMe() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.id").value(user1Id))
                .andExpect(jsonPath("$.data.profile.name").value("Artist One"));
    }

    @Test
    @Order(7)
    public void testEmptyOpportunitiesFeedInitially() throws Exception {
        mockMvc.perform(get("/api/opportunities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @Order(8)
    public void testCreatePostAndOpportunities() throws Exception {
        // Create standard post
        CreatePostRequest postReq = new CreatePostRequest();
        postReq.setType("post");
        postReq.setContent("Hello world from TrueHunt artist 1!");

        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("Hello world from TrueHunt artist 1!"))
                .andExpect(jsonPath("$.data.authorId").value(user1Id));

        // Create opportunity
        CreateOpportunityRequest oppReq = new CreateOpportunityRequest();
        oppReq.setTitle("Lead Vocalist Needed");
        oppReq.setContent("Looking for an energetic vocalist for upcoming album tour.");

        mockMvc.perform(post("/api/opportunities")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(oppReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("opportunity"))
                .andExpect(jsonPath("$.data.title").value("Lead Vocalist Needed"));

        // Verify opportunities feed now has the created opportunity
        mockMvc.perform(get("/api/opportunities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].title", hasItem("Lead Vocalist Needed")));

        // Verify posts feed has ordinary post
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].content", hasItem("Hello world from TrueHunt artist 1!")));
    }

    @Test
    @Order(9)
    public void testArticleRequiresTitle() throws Exception {
        CreatePostRequest articleReq = new CreatePostRequest();
        articleReq.setType("article");
        articleReq.setContent("Article content without a title...");

        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(articleReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @Order(10)
    public void testConnectionFlow() throws Exception {
        // Suggestions for user 1 should include user 2
        mockMvc.perform(get("/api/connections/suggestions")
                .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        // User 1 requests connection to User 2
        MvcResult connResult = mockMvc.perform(post("/api/connections")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientId\":\"" + user2Id + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andReturn();

        String connId = objectMapper.readTree(connResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // User 2 should have received a notification
        mockMvc.perform(get("/api/notifications")
                .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].type").value("connection_request"));

        // User 2 accepts connection
        mockMvc.perform(patch("/api/connections/" + connId)
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"accepted\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("accepted"));

        // User 1 accepted connections should now show User 2
        mockMvc.perform(get("/api/connections")
                .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @Order(11)
    public void testMessagingFlow() throws Exception {
        // Start or get direct conversation between User 1 and User 2
        MvcResult convResult = mockMvc.perform(post("/api/conversations/direct/" + user2Id)
                .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn();

        String convId = objectMapper.readTree(convResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // User 1 sends message to User 2
        mockMvc.perform(post("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Hey Artist Two, loved your portfolio!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.body").value("Hey Artist Two, loved your portfolio!"))
                .andExpect(jsonPath("$.data.senderId").value(user1Id));

        // User 2 reads messages in conversation
        mockMvc.perform(get("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].body").value("Hey Artist Two, loved your portfolio!"));

        // User 2 should have received a notification for the message
        mockMvc.perform(get("/api/notifications")
                .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @Order(12)
    public void testMarkNotificationsAsRead() throws Exception {
        // Bulk mark read for User 2
        mockMvc.perform(patch("/api/notifications/read")
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markedReadCount").isNumber());
    }
}
