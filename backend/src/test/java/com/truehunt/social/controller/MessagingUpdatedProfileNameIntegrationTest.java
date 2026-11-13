package com.truehunt.social.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MessagingUpdatedProfileNameIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(30000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProfileRepository profileRepository;

    private String nextIndianPhone(char leadingDigit) {
        long suffix = Math.abs((System.nanoTime() + SEQUENCE.incrementAndGet()) % 1_000_000_000L);
        return leadingDigit + String.format("%09d", suffix);
    }

    private JsonNode registerUser(String fullName, String email) throws Exception {
        String body = """
                {
                  "fullName": "%s",
                  "email": "%s",
                  "phoneNumber": "%s",
                  "password": "Password@123",
                  "confirmPassword": "Password@123",
                  "role": "TALENT",
                  "craft": "Acting"
                }
                """.formatted(fullName, email, nextIndianPhone('9'));

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    @Test
    void registeredProfilesStayVisibleWithNameAndDetailsWhenLegacyProfileUsesEmailAsId() throws Exception {
        long stamp = System.nanoTime();
        String email = "profile.directory." + stamp + "@gmail.com";
        JsonNode user = registerUser("Registered Profile User", email);
        String token = user.path("token").asText();
        String publicUserId = user.path("userId").asText();

        ProfileEntity profile = profileRepository.findByEmail(email).orElseThrow();
        profile.setUserId(email);
        profile.setPrimaryCraft("Director");
        profileRepository.save(profile);

        mockMvc.perform(get("/api/profiles/directory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.userId == '" + publicUserId + "')].name")
                        .value(hasItem("Registered Profile User")));

        mockMvc.perform(get("/api/profiles/" + publicUserId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Registered Profile User"))
                .andExpect(jsonPath("$.data.userId").value(publicUserId))
                .andExpect(jsonPath("$.data.primaryCraft").value("Director"));

        profileRepository.delete(profile);
        mockMvc.perform(get("/api/profiles/directory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.userId == '" + publicUserId + "')].name")
                        .value(hasItem("Registered Profile User")));
        mockMvc.perform(get("/api/profiles/" + publicUserId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Registered Profile User"))
                .andExpect(jsonPath("$.data.userId").value(publicUserId));
    }

    @Test
    void receiverSeesUpdatedSenderNameInConversationsAndMessagesEvenAfterRenameAndStaleClientPayload() throws Exception {
        long stamp = System.nanoTime();
        String senderEmail = "sender." + stamp + "@gmail.com";
        String receiverEmail = "receiver." + stamp + "@gmail.com";

        JsonNode senderAuth = registerUser("vamsi alladi", senderEmail);
        String senderToken = senderAuth.path("token").asText();
        String senderUserId = senderAuth.path("userId").asText();
        String receiverToken = registerUser("Receiver User", receiverEmail).path("token").asText();
        String unrelatedUserToken = registerUser("Unrelated User", "unrelated." + stamp + "@gmail.com")
                .path("token").asText();

        // 1. Initial message creates conversation with old name "vamsi alladi"
        String firstMsgBody = """
                {
                  "senderId": "%s",
                  "senderName": "vamsi alladi",
                  "receiverId": "%s",
                  "receiverName": "Receiver User",
                  "content": "hi"
                }
                """.formatted(senderEmail, receiverEmail);

        MvcResult firstMsgResult = mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstMsgBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.senderName").value("vamsi alladi"))
                .andReturn();

        long conversationId = objectMapper.readTree(firstMsgResult.getResponse().getContentAsString())
                .path("data").path("conversationId").asLong();

        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + conversationId + ")]").exists());
        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + unrelatedUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
        mockMvc.perform(get("/api/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + unrelatedUserToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/messages/" + conversationId)
                        .header("Authorization", "Bearer " + unrelatedUserToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + unrelatedUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"This should stay private.\"}"))
                .andExpect(status().isUnauthorized());

        // 2. Sender updates profile name from "vamsi alladi" to "Alladi Vamsi"
        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + senderToken)
                        .header("X-User-Email", senderEmail)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + senderUserId + "\",\"name\":\"Alladi Vamsi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Alladi Vamsi"));

        // 3. Even before sending another message, receiver's conversation list shows live updated name
        mockMvc.perform(get("/api/conversations")
                        .param("mailbox", "Focused")
                        .header("Authorization", "Bearer " + receiverToken)
                        .header("X-User-Email", receiverEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + conversationId + ")].name", hasItem("Alladi Vamsi")))
                .andExpect(jsonPath("$.data[?(@.id == " + conversationId + ")].initials", hasItem("AV")));

        // 4. Sender sends a new message even carrying stale senderName="vamsi alladi" from client cache
        String secondMsgBody = """
                {
                  "conversationId": %d,
                  "senderId": "%s",
                  "senderName": "vamsi alladi",
                  "receiverId": "%s",
                  "receiverName": "Receiver User",
                  "content": "hello"
                }
                """.formatted(conversationId, senderEmail, receiverEmail);

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondMsgBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.senderName").value("Alladi Vamsi"));

        // 5. Receiver fetches conversations and sees "Alladi Vamsi" on both conversation and thread entries
        mockMvc.perform(get("/api/conversations")
                        .param("mailbox", "Focused")
                        .header("Authorization", "Bearer " + receiverToken)
                        .header("X-User-Email", receiverEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + conversationId + ")].name", hasItem("Alladi Vamsi")))
                .andExpect(jsonPath("$.data[?(@.id == " + conversationId + ")].user1Name", hasItem("Alladi Vamsi")))
                .andExpect(jsonPath("$.data[?(@.id == " + conversationId + ")].thread[0].senderName", hasItem("Alladi Vamsi")))
                .andExpect(jsonPath("$.data[?(@.id == " + conversationId + ")].thread[1].senderName", hasItem("Alladi Vamsi")));
    }
}
