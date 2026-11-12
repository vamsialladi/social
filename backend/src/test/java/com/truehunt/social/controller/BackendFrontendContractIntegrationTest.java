package com.truehunt.social.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truehunt.social.model.JobEntity;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BackendFrontendContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProtectProjectRepository protectProjectRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private InvestmentInterestRepository investmentInterestRepository;

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void authMeResolvesJwtTokenAndXUserIdAndOtpSupportsPhoneNumber() throws Exception {
        String phoneDigits = "9" + String.format("%09d", Math.abs(System.nanoTime() % 1_000_000_000L));
        String formattedPhone = "+91 " + phoneDigits;

        // Send OTP using phoneNumber payload as sent by JoinNowPage.jsx
        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + formattedPhone + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value(org.hamcrest.Matchers.containsString(phoneDigits)));

        // Register a new TALENT user with production/experience fields
        String email = "contract.talent." + System.nanoTime() + "@truehunt.in";
        String regPayload = """
                {
                  "fullName": "Kavya Menon",
                  "email": "%s",
                  "phoneNumber": "%s",
                  "password": "Password@123",
                  "role": "TALENT",
                  "craft": "Screenplay Writer",
                  "subCraft": "Dialogue Writer",
                  "experienceYears": "3 - 5 Years",
                  "secondaryCrafts": ["Director"],
                  "secondarySubCrafts": ["Associate Director"]
                }
                """.formatted(email, formattedPhone);

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(regPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.userId").isNotEmpty())
                .andReturn();

        JsonNode regData = objectMapper.readTree(regResult.getResponse().getContentAsString()).path("data");
        String token = regData.path("token").asText();
        String userId = regData.path("userId").asText();
        assertTrue(userId.startsWith("thscKM"), "Expected thscKM prefix, got: " + userId);

        // /api/auth/me with ONLY Authorization: Bearer <token>
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.publicUserId").value(userId));

        // /api/auth/me with ONLY X-User-Id
        mockMvc.perform(get("/api/auth/me")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));

        // /api/settings with ONLY X-User-Id
        mockMvc.perform(get("/api/settings")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void profileSubEndpointsAcceptXUserIdAndProfileDtoIncludesPostsAndSuggestionsSupportUserIdParam() throws Exception {
        String email = "contract.profile." + System.nanoTime() + "@truehunt.in";
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rishi Dev",
                                  "email": "%s",
                                  "password": "Password@123",
                                  "role": "TALENT",
                                  "primaryCraft": "Direction"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        String userId = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .path("data").path("userId").asText();

        // Add experience, skill, and post using ONLY X-User-Id header
        mockMvc.perform(post("/api/profiles/experience")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Associate Director",
                                  "company": "Studio Nine",
                                  "location": "Hyderabad",
                                  "startDate": "2024"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Associate Director"));

        mockMvc.perform(post("/api/profiles/skills")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skill\":\"Storyboarding\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skills").isArray());

        mockMvc.perform(post("/api/profiles/posts")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Locked final storyboard for our thriller!\",\"type\":\"post\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Locked final storyboard for our thriller!"));

        // Verify GET /api/profiles/me returns populated posts, experiences, and skills
        mockMvc.perform(get("/api/profiles/me")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.posts").isArray())
                .andExpect(jsonPath("$.data.posts[0].content").value("Locked final storyboard for our thriller!"))
                .andExpect(jsonPath("$.data.experiences[0].title").value("Associate Director"));

        // Verify suggestions accept ?userId=
        mockMvc.perform(get("/api/profiles/suggestions")
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void talentDtoIncludesUserIdEmailConnectionFieldsAndMatchesCraftSynonyms() throws Exception {
        // Check Sound Design synonym matching (t-5 has Audiography / Sound Design)
        mockMvc.perform(get("/api/talents")
                        .param("category", "Sound Design"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='t-5')]").exists());

        // Check registered user lookup by userId and TalentDto fields
        mockMvc.perform(get("/api/talents/t-1")
                        .header("X-User-Email", "pro@truehunt.in"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("t-1"))
                .andExpect(jsonPath("$.data.userId").value("t-1"))
                .andExpect(jsonPath("$.data.isConnected").isBoolean())
                .andExpect(jsonPath("$.data.connected").isBoolean())
                .andExpect(jsonPath("$.data.connectionStatus").isNotEmpty());
    }

    @Test
    void protectProducerNormalizesCategoriesCreatesFullDefaultsAndPersistsToDatabase() throws Exception {
        // Categories expose frontend fields
        mockMvc.perform(get("/api/protect-producer/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("movies"))
                .andExpect(jsonPath("$.data[0].title").value("Movies"))
                .andExpect(jsonPath("$.data[0].tagline").isNotEmpty())
                .andExpect(jsonPath("$.data[0].workflowFocus").isNotEmpty());

        // Create project with plural category "MOVIES" as sent by ProtectCategoryPage.jsx
        MvcResult movieRes = mockMvc.perform(post("/api/protect-producer/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "MOVIES",
                                  "title": "Midnight Horizon",
                                  "productionCompany": "Blue Horizon Films",
                                  "producerName": "Anirudh Rao",
                                  "producerEmail": "anirudh@bluehorizon.in",
                                  "synopsis": "Sci-fi noir feature",
                                  "totalBudget": "₹8,00,00,000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("MOVIE"))
                .andExpect(jsonPath("$.data.tasks.length()").value(3))
                .andExpect(jsonPath("$.data.documents.length()").value(1))
                .andExpect(jsonPath("$.data.milestones.length()").value(4))
                .andReturn();

        String createdMovieId = objectMapper.readTree(movieRes.getResponse().getContentAsString())
                .path("data").path("id").asText();
        assertTrue(protectProjectRepository.findById(createdMovieId).isPresent());

        // Filtering by category=MOVIE returns the created project
        mockMvc.perform(get("/api/protect-producer/projects")
                        .param("category", "MOVIE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + createdMovieId + "')]").exists());

        // Create WEB_SERIES project -> auto-creates default episode
        MvcResult seriesRes = mockMvc.perform(post("/api/protect-producer/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "WEB_SERIES",
                                  "title": "Code Deccan",
                                  "seasonNumber": 1,
                                  "totalEpisodes": 8
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.episodes.length()").value(1))
                .andReturn();

        String createdSeriesId = objectMapper.readTree(seriesRes.getResponse().getContentAsString())
                .path("data").path("id").asText();
        assertTrue(protectProjectRepository.findById(createdSeriesId).isPresent());
    }

    @Test
    void jobsInvestorsAndSupportSubmissionsPersistToJpaRepositories() throws Exception {
        JobEntity beforeJob = jobRepository.findById("job-1").orElseThrow();
        int initialApps = beforeJob.getApplicationsCount();

        mockMvc.perform(post("/api/jobs/job-1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicantName": "Meera Nair",
                                  "applicantEmail": "meera@cinema.in",
                                  "phone": "+91 9876543210",
                                  "portfolioLink": "https://truehunt.in/reel",
                                  "pitchNote": "Excited to audition for the lead role."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        JobEntity afterJob = jobRepository.findById("job-1").orElseThrow();
        assertEquals(initialApps + 1, afterJob.getApplicationsCount());
        assertFalse(jobApplicationRepository.findAll().isEmpty());

        mockMvc.perform(post("/api/investment-interests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "investorId": "inv-1",
                                  "investorName": "Kalaari Cinema Ventures",
                                  "projectTitle": "Project Monsoon",
                                  "producerName": "Vikram Roy",
                                  "producerEmail": "vikram@aurafilms.in",
                                  "budgetRequested": "₹12 Cr",
                                  "synopsis": "Action drama pitch"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
        assertFalse(investmentInterestRepository.findAll().isEmpty());

        mockMvc.perform(post("/api/support")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Surya Prakash",
                                  "email": "surya@truehunt.in",
                                  "category": "Verification",
                                  "priority": "HIGH",
                                  "description": "Need badge verification assistance"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"));
        assertFalse(supportTicketRepository.findAll().isEmpty());
    }

    @Test
    void messagesAndConversationsResolveXUserIdAndJwtToken() throws Exception {
        String senderEmail = "msg.sender." + System.nanoTime() + "@truehunt.in";
        String regPayload = """
                {
                  "fullName": "Rohan Verma",
                  "email": "%s",
                  "password": "Password@123",
                  "role": "TALENT",
                  "craft": "Director"
                }
                """.formatted(senderEmail);

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(regPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode regData = objectMapper.readTree(regResult.getResponse().getContentAsString()).path("data");
        String token = regData.path("token").asText();
        String userId = regData.path("userId").asText();

        MvcResult sendRes = mockMvc.perform(post("/api/messages")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverId": "arjun.kapoor@truehunt.in",
                                  "content": "Interested in collaborating on your next project."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.senderId").value(senderEmail))
                .andExpect(jsonPath("$.data.conversationId").isNumber())
                .andReturn();

        long conversationId = objectMapper.readTree(sendRes.getResponse().getContentAsString())
                .path("data").path("conversationId").asLong();

        // Reading messages with only X-User-Id resolves outgoing direction properly
        mockMvc.perform(get("/api/messages/" + conversationId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].direction").value("outgoing"))
                .andExpect(jsonPath("$.data[0].content").value("Interested in collaborating on your next project."));

        // Listing conversations with only Authorization: Bearer <token> works
        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Conversation actions preserve caller perspective
        mockMvc.perform(patch("/api/conversations/" + conversationId + "/star")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.starred").value(true))
                .andExpect(jsonPath("$.data.name").value("arjun.kapoor@truehunt.in"));

        mockMvc.perform(patch("/api/conversations/" + conversationId + "/mailbox")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mailbox\":\"Other\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mailbox").value("Other"))
                .andExpect(jsonPath("$.data.name").value("arjun.kapoor@truehunt.in"));

        mockMvc.perform(patch("/api/conversations/" + conversationId + "/read")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unread").value(false))
                .andExpect(jsonPath("$.data.name").value("arjun.kapoor@truehunt.in"));
    }

    @Test
    void profileMeAndFieldsAndMediaKindAndScopedSettingsNotificationsMatchFrontendContract() throws Exception {
        String emailA = "contract.a." + System.nanoTime() + "@truehunt.in";
        String emailB = "contract.b." + System.nanoTime() + "@truehunt.in";

        MvcResult regA = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Kiran Rao",
                                  "email": "%s",
                                  "password": "Password@123",
                                  "role": "TALENT",
                                  "craft": "Editor"
                                }
                                """.formatted(emailA)))
                .andExpect(status().isOk())
                .andReturn();
        String userIdA = objectMapper.readTree(regA.getResponse().getContentAsString())
                .path("data").path("userId").asText();

        MvcResult regB = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Neha Sharma",
                                  "email": "%s",
                                  "password": "Password@123",
                                  "role": "TALENT",
                                  "craft": "Director"
                                }
                                """.formatted(emailB)))
                .andExpect(status().isOk())
                .andReturn();
        String userIdB = objectMapper.readTree(regB.getResponse().getContentAsString())
                .path("data").path("userId").asText();

        // /api/profile/me works with X-User-Id and maps experience/education fields cleanly
        mockMvc.perform(post("/api/profile/experience")
                        .header("X-User-Id", userIdA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Lead Editor",
                                  "company": "Prism Films",
                                  "movieName": "Skyline",
                                  "duration": "2023 - Present",
                                  "isCurrent": true
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/profile/education")
                        .header("X-User-Id", userIdA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "institution": "FTII",
                                  "degree": "PG Diploma",
                                  "activities": "Editing Club"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/profile/me")
                        .header("X-User-Id", userIdA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.experiences[0].isCurrent").value(true))
                .andExpect(jsonPath("$.data.experiences[0].duration").value("2023 - Present"))
                .andExpect(jsonPath("$.data.experiences[0].movieName").value("Skyline"))
                .andExpect(jsonPath("$.data.educations[0].activities").value("Editing Club"));

        // Media upload returns kind based on contentType
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "sample.png",
                "image/png",
                "png-bytes".getBytes()
        );
        mockMvc.perform(multipart("/api/media/upload")
                        .file(imageFile)
                        .param("category", "post"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kind").value("image"))
                .andExpect(jsonPath("$.data.category").value("post"));

        // Per-user settings do not leak between users
        mockMvc.perform(patch("/api/settings/theme")
                        .header("X-User-Id", userIdA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\":\"light\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.theme").value("light"));

        mockMvc.perform(get("/api/settings")
                        .header("X-User-Id", userIdA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.theme").value("light"));

        mockMvc.perform(get("/api/settings")
                        .header("X-User-Id", userIdB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.theme").value("dark"));

        // Per-user notifications filter and mark-all-read by caller
        notificationRepository.save(new NotificationEntity(emailA, "Title A", "For A only", "INFO"));
        NotificationEntity notifB = notificationRepository.save(new NotificationEntity(emailB, "Title B", "For B only", "INFO"));

        mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", userIdA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.title=='Title A')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.title=='Title B')]").isEmpty());

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("X-User-Id", userIdA))
                .andExpect(status().isOk());

        NotificationEntity reloadedB = notificationRepository.findById(notifB.getId()).orElseThrow();
        assertFalse(reloadedB.isRead());
    }
}

