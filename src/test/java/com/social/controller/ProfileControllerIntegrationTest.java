package com.social.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.dto.request.ProfileUpdateRequest;
import com.social.entity.ProfileVisibility;
import com.social.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProfileRepository profileRepository;

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
    }

    @Test
    void updateMyProfile_andGetProfile_privacyRulesApplied() throws Exception {
        ProfileUpdateRequest updateRequest = ProfileUpdateRequest.builder()
                .name("Jane Visual Artist")
                .phone("+1-555-0199")
                .email("jane@vfxartist.com")
                .experience("8 years senior animator")
                .languages(List.of("English", "French"))
                .talents(List.of("3D Animation", "Lighting", "Compositing"))
                .visibility(ProfileVisibility.PUBLIC)
                .build();

        // 1. Update signed-in user's profile
        mockMvc.perform(patch("/api/profiles/me")
                        .header("X-User-Id", "artist-jane")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Jane Visual Artist")))
                .andExpect(jsonPath("$.data.phone", is("+1-555-0199")))
                .andExpect(jsonPath("$.data.email", is("jane@vfxartist.com")));

        // 2. An unauthorized viewer views Jane's profile -> phone & email must be masked/null
        mockMvc.perform(get("/api/profiles/artist-jane")
                        .header("X-User-Id", "random-viewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Jane Visual Artist")))
                .andExpect(jsonPath("$.data.experience", is("8 years senior animator")))
                .andExpect(jsonPath("$.data.phone").doesNotExist())
                .andExpect(jsonPath("$.data.email").doesNotExist());

        // 3. Jane views her own profile -> phone & email are present
        mockMvc.perform(get("/api/profiles/artist-jane")
                        .header("X-User-Id", "artist-jane"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.phone", is("+1-555-0199")))
                .andExpect(jsonPath("$.data.email", is("jane@vfxartist.com")));
    }

    @Test
    void getPrivateProfile_byOtherUser_returnsForbidden() throws Exception {
        ProfileUpdateRequest updateRequest = ProfileUpdateRequest.builder()
                .name("Private Artist")
                .visibility(ProfileVisibility.PRIVATE)
                .build();

        mockMvc.perform(patch("/api/profiles/me")
                        .header("X-User-Id", "private-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/profiles/private-user")
                        .header("X-User-Id", "another-user"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("This profile is private")));
    }
}
