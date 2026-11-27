package com.truehunt.social.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CalendarControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void calendarEntriesArePrivateAndPersistAcrossApiRequests() throws Exception {
        String ownerToken = registerAndGetToken("calendar.owner");
        String otherToken = registerAndGetToken("calendar.other");
        String entryId = "calendar-" + System.nanoTime();

        mockMvc.perform(post("/api/calendar/entries")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "%s",
                                  "title": "Project meeting",
                                  "type": "Reminder",
                                  "date": "2030-07-14",
                                  "time": "10:30",
                                  "details": "Bring the latest draft",
                                  "completed": false
                                }
                                """.formatted(entryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(entryId))
                .andExpect(jsonPath("$.data.date").value("2030-07-14"));

        mockMvc.perform(get("/api/calendar/entries")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/calendar/entries")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Project meeting"));

        mockMvc.perform(delete("/api/calendar/entries/" + entryId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/calendar/entries/" + entryId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    @Test
    void calendarApiRequiresJwtRatherThanIdentityHeaders() throws Exception {
        mockMvc.perform(get("/api/calendar/entries").header("X-User-Id", "some-user"))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndGetToken(String prefix) throws Exception {
        String email = prefix + "." + System.nanoTime() + "@truehunt.in";
        String phoneNumber = "9" + String.format(
                "%09d",
                Math.floorMod(System.nanoTime(), 1_000_000_000L)
        );
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Calendar User",
                                  "email": "%s",
                                  "phoneNumber": "%s",
                                  "password": "Password@123",
                                  "role": "TALENT"
                                }
                                """.formatted(email, phoneNumber)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        String token = data.path("token").asText();
        assertTrue(!token.isBlank());
        return token;
    }
}
