package com.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.config.UserContextInterceptor;
import com.social.dto.request.CreateOpportunityRequest;
import com.social.dto.request.CreatePostRequest;
import com.social.dto.request.CreateUserRequest;
import com.social.dto.response.UserProfileResponse;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpportunityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private UserProfileResponse testAuthor;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();

        testAuthor = userService.createUser(CreateUserRequest.builder()
                .username("casting_director")
                .name("Sarah Director")
                .email("sarah@casting.com")
                .headline("Lead Casting Director at CineWorks")
                .role("EMPLOYER")
                .location("Mumbai, India")
                .build());
    }

    @Test
    @DisplayName("GET /api/opportunities returns empty list and no mock jobs when no records exist")
    void testGetOpportunitiesEmptyState() throws Exception {
        mockMvc.perform(get("/api/opportunities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.hasMore", is(false)))
                .andExpect(jsonPath("$.totalCount", is(0)));
    }

    @Test
    @DisplayName("POST /api/opportunities forces type=opportunity, sets author and saves opportunity")
    void testCreateOpportunitySuccess() throws Exception {
        CreateOpportunityRequest request = CreateOpportunityRequest.builder()
                .title("Lead Actor for Feature Film")
                .content("Seeking experienced male actor aged 25-35 for high-budget indie thriller.")
                .employer("CineWorks Studio")
                .compensation("₹500,000 fixed + profit share")
                .location("Hyderabad, India")
                .expiryDate(Instant.now().plus(30, ChronoUnit.DAYS))
                .mediaUrls(List.of("https://cdn.example.com/brief.pdf", "https://cdn.example.com/poster.jpg"))
                .build();

        mockMvc.perform(post("/api/opportunities")
                        .header(UserContextInterceptor.USER_ID_HEADER, testAuthor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Lead Actor for Feature Film")))
                .andExpect(jsonPath("$.data.type", is("opportunity")))
                .andExpect(jsonPath("$.data.authorId", is(testAuthor.getId())))
                .andExpect(jsonPath("$.data.authorName", is("Sarah Director")))
                .andExpect(jsonPath("$.data.employer", is("CineWorks Studio")))
                .andExpect(jsonPath("$.data.mediaUrls", hasSize(2)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data._id", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/opportunities rejects missing required fields (explicit schema validation)")
    void testCreateOpportunityValidationFailure() throws Exception {
        CreateOpportunityRequest invalidRequest = CreateOpportunityRequest.builder()
                .title("") // empty title
                .content("") // empty content
                .build();

        mockMvc.perform(post("/api/opportunities")
                        .header(UserContextInterceptor.USER_ID_HEADER, testAuthor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.title", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.content", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/opportunities rejects request without authentication header")
    void testCreateOpportunityWithoutAuthHeader() throws Exception {
        CreateOpportunityRequest request = CreateOpportunityRequest.builder()
                .title("Sound Engineer")
                .content("Looking for a sound mixer")
                .build();

        mockMvc.perform(post("/api/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("Rule: Opportunity posts must not appear in ordinary posts feed")
    void testOpportunityExcludedFromOrdinaryFeed() throws Exception {
        // 1. Create an opportunity
        CreateOpportunityRequest oppRequest = CreateOpportunityRequest.builder()
                .title("Cinematographer Wanted")
                .content("Documentary shoot in Ladakh")
                .build();

        mockMvc.perform(post("/api/opportunities")
                        .header(UserContextInterceptor.USER_ID_HEADER, testAuthor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oppRequest)))
                .andExpect(status().isCreated());

        // 2. Create a standard social post
        CreatePostRequest postRequest = CreatePostRequest.builder()
                .content("Behind the scenes from our latest teaser shoot! What do you guys think?")
                .mediaUrls(List.of("https://cdn.example.com/bts.jpg"))
                .build();

        mockMvc.perform(post("/api/posts")
                        .header(UserContextInterceptor.USER_ID_HEADER, testAuthor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isCreated());

        // 3. Verify GET /api/opportunities returns ONLY the opportunity
        mockMvc.perform(get("/api/opportunities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].type", is("opportunity")))
                .andExpect(jsonPath("$.items[0].title", is("Cinematographer Wanted")));

        // 4. Verify GET /api/posts returns ONLY the ordinary post and excludes the opportunity
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].type", is("post")))
                .andExpect(jsonPath("$.items[0].content", containsString("Behind the scenes")));
    }
}
