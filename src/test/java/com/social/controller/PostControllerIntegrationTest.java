package com.social.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.dto.request.PostCreateRequest;
import com.social.dto.request.PostUpdateRequest;
import com.social.entity.PostType;
import com.social.entity.PostVisibility;
import com.social.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PostControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
    }

    @Test
    void createPost_withoutUserHeader_returnsUnauthorized() throws Exception {
        PostCreateRequest request = PostCreateRequest.builder()
                .type(PostType.POST)
                .content("Test content without user")
                .build();

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    void createPost_regularPost_returnsCreated() throws Exception {
        PostCreateRequest request = PostCreateRequest.builder()
                .type(PostType.POST)
                .content("Valid post content")
                .visibility(PostVisibility.PUBLIC)
                .build();

        mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", "user-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.authorId", is("user-123")))
                .andExpect(jsonPath("$.data.content", is("Valid post content")))
                .andExpect(jsonPath("$.meta.nextCursor").value(nullValue()));
    }

    @Test
    void createPost_articleWithoutTitle_returnsBadRequest() throws Exception {
        PostCreateRequest request = PostCreateRequest.builder()
                .type(PostType.ARTICLE)
                .content("Article body without required title")
                .build();

        mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", "user-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("INVALID_INPUT")))
                .andExpect(jsonPath("$.error.message", containsString("Title is required for articles")));
    }

    @Test
    void getFeed_returnsVisiblePostsExcludingOpportunities() throws Exception {
        // Create an article
        PostCreateRequest articleReq = PostCreateRequest.builder()
                .type(PostType.ARTICLE)
                .title("Creative Guide")
                .content("Content of creative guide")
                .build();

        mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", "user-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(articleReq)))
                .andExpect(status().isCreated());

        // Create an opportunity post (should be excluded from feed)
        PostCreateRequest oppReq = PostCreateRequest.builder()
                .type(PostType.OPPORTUNITY)
                .content("Job opportunity for animators")
                .build();

        mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", "user-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oppReq)))
                .andExpect(status().isCreated());

        // Call GET /api/posts feed
        mockMvc.perform(get("/api/posts?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].type", is("article")))
                .andExpect(jsonPath("$.meta.nextCursor").value(nullValue()));
    }

    @Test
    void updatePost_byNonAuthor_returnsForbidden() throws Exception {
        PostCreateRequest createReq = PostCreateRequest.builder()
                .type(PostType.POST)
                .content("Original content")
                .build();

        String responseJson = mockMvc.perform(post("/api/posts")
                        .header("X-User-Id", "author-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String postId = objectMapper.readTree(responseJson).get("data").get("id").asText();

        PostUpdateRequest updateReq = PostUpdateRequest.builder()
                .content("Attempted unauthorized modification")
                .build();

        mockMvc.perform(patch("/api/posts/" + postId)
                        .header("X-User-Id", "intruder-99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
    }
}
