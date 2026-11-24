package com.truehunt.social.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truehunt.social.model.JobApplicationEntity;
import com.truehunt.social.model.OpportunityApplicationEntity;
import com.truehunt.social.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NewFrontendFeaturesContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OpportunityApplicationRepository opportunityApplicationRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private InvestmentInterestRepository investmentInterestRepository;

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Test
    void actuatorHealthEndpointIsPubliclyAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private String registerAndGetToken(String name, String email, String phone) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", name,
                                "email", email,
                                "phoneNumber", phone,
                                "password", "TrueHunt2026!",
                                "confirmPassword", "TrueHunt2026!",
                                "role", "TALENT",
                                "craft", "Acting"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }

    private String loginAdminToken() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "admin@test.com",
                                "password", "admin123"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    @Test
    void persistedEndorsementsAreIncludedWhenProfilesAreLoaded() throws Exception {
        long suffix = System.nanoTime();
        String ownerEmail = "endorsement.owner." + suffix + "@truehunt.in";
        String writerEmail = "endorsement.writer." + suffix + "@truehunt.in";
        String ownerToken = registerAndGetToken(
                "Endorsement Owner",
                ownerEmail,
                "9" + String.format("%09d", Math.floorMod(suffix, 1_000_000_000L))
        );
        String writerToken = registerAndGetToken(
                "Endorsement Writer",
                writerEmail,
                "8" + String.format("%09d", Math.floorMod(suffix + 1, 1_000_000_000L))
        );
        MvcResult ownerProfileResult = mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        String ownerProfileId = objectMapper.readTree(ownerProfileResult.getResponse().getContentAsString())
                .path("data").path("userId").asText();

        mockMvc.perform(post("/api/profiles/" + ownerProfileId + "/endorsements")
                        .header("Authorization", "Bearer " + writerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "endorsementMessage", "A dependable collaborator.",
                                "visibility", "Public"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endorsementMessage").value("A dependable collaborator."));

        mockMvc.perform(get("/api/profiles/" + ownerProfileId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endorsements[0].endorsementMessage")
                        .value("A dependable collaborator."));
    }

    @Test
    void persistentSettingsAndBlockedUsersWorkEndToEnd() throws Exception {
        String email = "settings.user." + System.nanoTime() + "@truehunt.in";
        String token = registerAndGetToken("Settings Tester", email, "9123456001");

        mockMvc.perform(patch("/api/settings/theme")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\":\"light\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.theme").value("light"));

        mockMvc.perform(patch("/api/settings/privacy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileVisibility\":false,\"activityStatus\":true,\"searchVisibility\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.privacyPreferences.profileVisibility").value(false));

        mockMvc.perform(patch("/api/settings/restricted")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sensitiveContent\":false,\"restrictedMessages\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restrictedPreferences.sensitiveContent").value(false));

        mockMvc.perform(patch("/api/settings/media-visibility")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoPosts\":\"private\",\"videoPosts\":\"public\",\"externalUrls\":\"private\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mediaPreferences.photoPosts").value("private"));

        mockMvc.perform(patch("/api/settings/engagement")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"likeCounts\":false,\"shareCounts\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.engagementPreferences.likeCounts").value(false));

        mockMvc.perform(patch("/api/settings/security")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"twoFactorEnabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.twoFactorEnabled").value(true));

        mockMvc.perform(post("/api/settings/blocked-users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identity\":\"spammer@truehunt.in\",\"name\":\"Spammer\",\"email\":\"spammer@truehunt.in\",\"reason\":\"Spam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.identity").value("spammer@truehunt.in"));

        mockMvc.perform(get("/api/settings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.theme").value("light"))
                .andExpect(jsonPath("$.data.twoFactorEnabled").value(true))
                .andExpect(jsonPath("$.data.notifications.connections").value(true))
                .andExpect(jsonPath("$.data.notifications['network-catch-up']").value(true))
                .andExpect(jsonPath("$.data.privacyPreferences.profileVisibility").value(false))
                .andExpect(jsonPath("$.data.mediaPreferences.photoPosts").value("private"))
                .andExpect(jsonPath("$.data.engagementPreferences.shareCounts").value(false))
                .andExpect(jsonPath("$.data.blockedUsers[0].identity").value("spammer@truehunt.in"));

        mockMvc.perform(delete("/api/settings/blocked-users")
                        .param("identity", "spammer@truehunt.in")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unblocked").value(true));
    }

    @Test
    void postAudienceSaveReportShareHideAndOpportunityFieldsWork() throws Exception {
        long suffix = System.nanoTime();
        String authorEmail = "author." + suffix + "@truehunt.in";
        String viewerEmail = "viewer." + suffix + "@truehunt.in";
        String authorToken = registerAndGetToken("Author One", authorEmail, "9123456002");
        String viewerToken = registerAndGetToken("Viewer Two", viewerEmail, "9123456003");

        // Create private post — should only be visible to author
        MvcResult privRes = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"My private script notes\",\"type\":\"post\",\"audience\":\"private\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.audience").value("private"))
                .andReturn();
        String privatePostId = objectMapper.readTree(privRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Viewer should NOT see private post in feed
        MvcResult viewerFeed = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn();
        assertFalse(viewerFeed.getResponse().getContentAsString().contains("My private script notes"));

        // Partial PATCH updates audience without wiping content
        mockMvc.perform(patch("/api/posts/" + privatePostId)
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"audience\":\"public\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.audience").value("public"))
                .andExpect(jsonPath("$.data.content").value("My private script notes"));

        // Viewer can now save, share, report, and mark not-interested
        mockMvc.perform(post("/api/posts/" + privatePostId + "/save")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved").value(true));

        mockMvc.perform(get("/api/posts/saved")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(privatePostId))
                .andExpect(jsonPath("$.data[0].ownerRemoved").value(false));

        mockMvc.perform(post("/api/posts/" + privatePostId + "/share")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sharesCount").value(1));

        mockMvc.perform(post("/api/posts/" + privatePostId + "/report")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Inappropriate content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        mockMvc.perform(get("/api/reports/me")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].postId").value(privatePostId));

        String adminToken = loginAdminToken();
        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].postId").value(privatePostId));

        mockMvc.perform(post("/api/posts/" + privatePostId + "/not-interested")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hidden").value(true));

        // Opportunity application with applicantName & experience
        MvcResult oppRes = mockMvc.perform(post("/api/opportunities")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Lead Actor Call\",\"content\":\"Feature film lead\",\"type\":\"opportunity\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Long oppId = Long.parseLong(objectMapper.readTree(oppRes.getResponse().getContentAsString())
                .path("data").path("id").asText());

        mockMvc.perform(post("/api/posts/" + oppId + "/apply")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicantName\":\"Viewer Two\",\"experience\":\"6 Years\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applied").value(true));

        OpportunityApplicationEntity savedApp = opportunityApplicationRepository
                .findByPostIdAndApplicantIdentity(oppId, viewerEmail.toLowerCase())
                .orElseThrow();
        assertEquals("Viewer Two", savedApp.getApplicantName());
        assertEquals("6 Years", savedApp.getExperience());
    }

    @Test
    void contactLcmChecklistsFollowersAndDroppedEntityFieldsPersist() throws Exception {
        // Contact Us
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ravi Kumar\",\"email\":\"ravi@truehunt.in\",\"phone\":\"9876543211\",\"subject\":\"Studio Inquiry\",\"message\":\"Need studio onboarding.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));

        // LCM / Zine Wood Beta Signup
        mockMvc.perform(post("/api/lcm/beta-signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"beta.producer@truehunt.in\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("JOINED"));

        // Protect Producer Checklists
        MvcResult projRes = mockMvc.perform(post("/api/protect-producer/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"MOVIES\",\"title\":\"Checklist Film\",\"producerName\":\"Test Producer\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String projectId = objectMapper.readTree(projRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(patch("/api/protect-producer/projects/" + projectId + "/checklists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "readinessChecklist", List.of(Map.of("id", "rc-1", "label", "Script Locked", "checked", true))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checklists.readinessChecklist[0].checked").value(true));

        // Job Application extra fields persisted
        MvcResult jobAppRes = mockMvc.perform(post("/api/jobs/job-1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicantName\":\"Asha\",\"applicantEmail\":\"asha@truehunt.in\",\"experience\":\"5 Years\",\"resumeUrl\":\"https://example.com/cv.pdf\",\"expectedPay\":\"5L\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String appId = objectMapper.readTree(jobAppRes.getResponse().getContentAsString())
                .path("data").path("applicationId").asText();
        JobApplicationEntity jobApp = jobApplicationRepository.findById(appId).orElseThrow();
        assertEquals("5 Years", jobApp.getExperience());
        assertEquals("https://example.com/cv.pdf", jobApp.getResumeUrl());
        assertEquals("5L", jobApp.getExpectedPay());

        // Investment Interest extra fields persisted
        MvcResult invRes = mockMvc.perform(post("/api/investment-interests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"investorId\":\"inv-1\",\"investorName\":\"Kalaari\",\"projectId\":\"p-9\",\"projectTitle\":\"Epic\",\"producerName\":\"Raj\",\"producerEmail\":\"raj@truehunt.in\",\"phone\":\"9876543219\",\"interestType\":\"CO_PRODUCTION\",\"budgetRequested\":\"10 Cr\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String interestId = objectMapper.readTree(invRes.getResponse().getContentAsString())
                .path("data").path("interestId").asText();
        var savedInterest = investmentInterestRepository.findById(interestId).orElseThrow();
        assertEquals("p-9", savedInterest.getProjectId());
        assertEquals("9876543219", savedInterest.getPhone());
        assertEquals("CO_PRODUCTION", savedInterest.getInterestType());

        // Support Ticket extra fields persisted
        MvcResult supRes = mockMvc.perform(post("/api/support")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Kiran\",\"email\":\"kiran@truehunt.in\",\"phone\":\"9876543218\",\"category\":\"Technical Bug\",\"subject\":\"Issue\",\"description\":\"Details\",\"timestamp\":\"2026-11-18T10:00:00Z\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String ticketId = objectMapper.readTree(supRes.getResponse().getContentAsString())
                .path("data").path("ticketId").asText();
        var savedTicket = supportTicketRepository.findById(ticketId).orElseThrow();
        assertEquals("9876543218", savedTicket.getPhone());
        assertEquals("2026-11-18T10:00:00Z", savedTicket.getClientTimestamp());

        // Connections followers & catch-up endpoints return 200
        long suffix = System.nanoTime();
        String userToken = registerAndGetToken("Net User", "net." + suffix + "@truehunt.in", "9123456004");
        mockMvc.perform(get("/api/connections/followers")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(get("/api/connections/catch-up")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void adminCanRevokePublishedInvestorListingAndHideConversationIsPerUser() throws Exception {
        long suffix = System.nanoTime();
        String u1Email = "inv.owner." + suffix + "@truehunt.in";
        String u2Email = "inv.peer." + suffix + "@truehunt.in";
        String u1Token = registerAndGetToken("Owner User", u1Email, "9123456005");
        String u2Token = registerAndGetToken("Peer User", u2Email, "9123456006");

        // Submit investor access request -> auto-PUBLISHED
        MvcResult reqRes = mockMvc.perform(post("/api/investor-access/requests")
                        .header("Authorization", "Bearer " + u1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Owner User\",\"budget\":\"5 Cr\",\"role\":\"Investor\",\"description\":\"Funding regional cinema\",\"consentAccepted\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andReturn();
        long listingId = objectMapper.readTree(reqRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Admin revokes published listing
        String adminToken = loginAdminToken();
        mockMvc.perform(post("/api/admin/investor-requests/" + listingId + "/reject")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        // Per-user hide conversation
        MvcResult convRes = mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + u1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "recipient", u2Email,
                                "messageText", "Hello peer"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        long convId = objectMapper.readTree(convRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/conversations/" + convId + "/hide")
                        .header("Authorization", "Bearer " + u1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hidden").value(true));

        // u1 no longer sees conversation, u2 still sees it!
        MvcResult u1Convs = mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + u1Token))
                .andExpect(status().isOk())
                .andReturn();
        assertFalse(u1Convs.getResponse().getContentAsString().contains("Hello peer"));

        MvcResult u2Convs = mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + u2Token))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(u2Convs.getResponse().getContentAsString().contains("Hello peer"));
    }

    @Test
    void mediaVisibilitySettingsHidePrivatePhotoVideoAndUrlPostsFromOtherUsers() throws Exception {
        long suffix = System.nanoTime();
        String ownerEmail = "vis.owner." + suffix + "@truehunt.in";
        String viewerEmail = "vis.viewer." + suffix + "@truehunt.in";
        String ownerToken = registerAndGetToken("Visibility Owner", ownerEmail, "9123456011");
        String viewerToken = registerAndGetToken("Visibility Viewer", viewerEmail, "9123456012");

        // Owner creates a photo post, a video post, a URL post, and a plain text post
        MvcResult photoRes = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"My private photo post\",\"type\":\"post\",\"media\":{\"url\":\"/uploads/set-photo.jpg\",\"kind\":\"image\"}}"))
                .andExpect(status().isOk())
                .andReturn();
        String photoPostId = objectMapper.readTree(photoRes.getResponse().getContentAsString()).path("data").path("id").asText();

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"My private video post\",\"type\":\"post\",\"media\":{\"url\":\"/uploads/showreel.mp4\",\"kind\":\"video\"}}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Check my portfolio at https://truehunt.in/reel\",\"type\":\"post\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Plain text update for everyone\",\"type\":\"post\"}"))
                .andExpect(status().isOk());

        // Owner sets Photo posts, Video posts, and External URLs to Private in Settings -> Visibility
        mockMvc.perform(patch("/api/settings/media-visibility")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoPosts\":\"private\",\"videoPosts\":\"private\",\"externalUrls\":\"private\"}"))
                .andExpect(status().isOk());

        // Owner still sees all 4 posts
        String ownerFeed = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(ownerFeed.contains("My private photo post"));
        assertTrue(ownerFeed.contains("My private video post"));
        assertTrue(ownerFeed.contains("https://truehunt.in/reel"));
        assertTrue(ownerFeed.contains("Plain text update for everyone"));

        // Another user does NOT see private photo, video, or URL posts, but DOES see plain text post
        String viewerFeed = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(viewerFeed.contains("My private photo post"));
        assertFalse(viewerFeed.contains("My private video post"));
        assertFalse(viewerFeed.contains("https://truehunt.in/reel"));
        assertTrue(viewerFeed.contains("Plain text update for everyone"));

        // Direct GET /api/posts/{id} for private photo post is hidden from viewer
        mockMvc.perform(get("/api/posts/" + photoPostId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isNotFound());

        // Explicitly editing that individual post to public makes it visible to everyone even while photoPosts setting is still private
        mockMvc.perform(patch("/api/posts/" + photoPostId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"audience\":\"public\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.audience").value("public"));

        String afterPostEditFeed = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(afterPostEditFeed.contains("My private photo post"));
        assertFalse(afterPostEditFeed.contains("My private video post"));

        // Switching photoPosts back to private in Settings hides it again, and back to public shows it again
        mockMvc.perform(patch("/api/settings/media-visibility")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoPosts\":\"private\"}"))
                .andExpect(status().isOk());
        String afterSettingsPrivateFeed = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(afterSettingsPrivateFeed.contains("My private photo post"));

        mockMvc.perform(patch("/api/settings/media-visibility")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoPosts\":\"public\"}"))
                .andExpect(status().isOk());

        String refreshedViewerFeed = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(refreshedViewerFeed.contains("My private photo post"));
        assertFalse(refreshedViewerFeed.contains("My private video post"));

        // Editing the post from public back to private hides it again from everyone
        mockMvc.perform(patch("/api/posts/" + photoPostId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"audience\":\"private\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.audience").value("private"));

        String afterEditBackToPrivateFeed = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(afterEditBackToPrivateFeed.contains("My private photo post"));
    }

    @Test
    void unregisteredEmailSessionsDoNotLeakPrivatePostsAcrossFallbackProfile() throws Exception {
        long suffix = System.nanoTime();
        String staleAuthor = "stale.author." + suffix + "@example.com";
        String staleViewer = "stale.viewer." + suffix + "@example.com";

        MvcResult createRes = mockMvc.perform(post("/api/posts")
                        .header("X-User-Email", staleAuthor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Stale session photo post\",\"type\":\"post\",\"audience\":\"public\",\"media\":{\"url\":\"/uploads/stale.jpg\",\"kind\":\"image\"}}"))
                .andExpect(status().isOk())
                .andReturn();
        String postId = objectMapper.readTree(createRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Visible to staleViewer while public
        String publicFeed = mockMvc.perform(get("/api/posts")
                        .header("X-User-Email", staleViewer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(publicFeed.contains("Stale session photo post"));

        // Switching post from public -> private hides it from staleViewer (no fallback-profile owner collision)
        mockMvc.perform(patch("/api/posts/" + postId)
                        .header("X-User-Email", staleAuthor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"audience\":\"private\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.audience").value("private"));

        String privateFeedForViewer = mockMvc.perform(get("/api/posts")
                        .header("X-User-Email", staleViewer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(privateFeedForViewer.contains("Stale session photo post"));

        String privateFeedForAuthor = mockMvc.perform(get("/api/posts")
                        .header("X-User-Email", staleAuthor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(privateFeedForAuthor.contains("Stale session photo post"));

        // Switching post back to public shows it again, then Settings -> Visibility Private hides it again
        mockMvc.perform(patch("/api/posts/" + postId)
                        .header("X-User-Email", staleAuthor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"audience\":\"public\"}"))
                .andExpect(status().isOk());
        String rePublicFeed = mockMvc.perform(get("/api/posts")
                        .header("X-User-Email", staleViewer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(rePublicFeed.contains("Stale session photo post"));

        mockMvc.perform(patch("/api/settings/media-visibility")
                        .header("X-User-Email", staleAuthor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoPosts\":\"private\"}"))
                .andExpect(status().isOk());
        String settingsPrivateFeed = mockMvc.perform(get("/api/posts")
                        .header("X-User-Email", staleViewer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(settingsPrivateFeed.contains("Stale session photo post"));
    }

    @Test
    void videoPostsAndProfileVideosRespectPublicPrivateAndVisibilitySettings() throws Exception {
        long suffix = System.nanoTime();
        String ownerEmail = "video.owner." + suffix + "@truehunt.in";
        String viewerEmail = "video.viewer." + suffix + "@truehunt.in";
        String ownerToken = registerAndGetToken("Video Owner", ownerEmail, "9123456021");
        String viewerToken = registerAndGetToken("Video Viewer", viewerEmail, "9123456022");

        // Fetch owner publicUserId
        MvcResult meRes = mockMvc.perform(get("/api/profile/me")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        String ownerPublicId = objectMapper.readTree(meRes.getResponse().getContentAsString())
                .path("data").path("userId").asText();

        // Owner adds a profile video & video URL
        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + ownerPublicId + "\",\"videos\":[{\"url\":\"/uploads/reel-1.mp4\",\"name\":\"Reel 1\"}],\"videoUrls\":[\"https://youtu.be/demo123\"]}"))
                .andExpect(status().isOk());

        // Owner creates a PUBLIC video post
        MvcResult videoPostRes = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Behind the scenes clip\",\"type\":\"post\",\"audience\":\"public\",\"media\":{\"url\":\"/uploads/bts.mp4\",\"kind\":\"video\",\"contentType\":\"video/mp4\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mediaType").value("video"))
                .andReturn();
        String videoPostId = objectMapper.readTree(videoPostRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // While public, viewer sees both the video post and profile videos
        String feedPublic = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(feedPublic.contains("Behind the scenes clip"));

        String profilePublic = mockMvc.perform(get("/api/profile/" + ownerPublicId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(profilePublic.contains("reel-1.mp4"));
        assertTrue(profilePublic.contains("https://youtu.be/demo123"));

        // Changing the video post from public -> private hides it from viewer, keeps it for owner
        mockMvc.perform(patch("/api/posts/" + videoPostId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"audience\":\"private\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.audience").value("private"));

        String feedPrivate = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(feedPrivate.contains("Behind the scenes clip"));

        String ownerFeedPrivate = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(ownerFeedPrivate.contains("Behind the scenes clip"));

        // Changing the video post back from private -> public shows it to viewer again
        mockMvc.perform(patch("/api/posts/" + videoPostId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"audience\":\"public\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.audience").value("public"));

        String feedRePublic = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(feedRePublic.contains("Behind the scenes clip"));

        // Settings -> Visibility -> Video posts = Private hides both video post AND profile videos from viewer
        mockMvc.perform(patch("/api/settings/media-visibility")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"videoPosts\":\"private\"}"))
                .andExpect(status().isOk());

        String feedSettingsPrivate = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(feedSettingsPrivate.contains("Behind the scenes clip"));

        String profileSettingsPrivate = mockMvc.perform(get("/api/profile/" + ownerPublicId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(profileSettingsPrivate.contains("reel-1.mp4"));
        assertFalse(profileSettingsPrivate.contains("https://youtu.be/demo123"));

        // Owner still sees their own profile videos
        String ownerProfilePrivate = mockMvc.perform(get("/api/profile/" + ownerPublicId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(ownerProfilePrivate.contains("reel-1.mp4"));

        // Settings -> Visibility -> Video posts = Public restores both video post AND profile videos for viewer
        mockMvc.perform(patch("/api/settings/media-visibility")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"videoPosts\":\"public\"}"))
                .andExpect(status().isOk());

        String feedSettingsPublic = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(feedSettingsPublic.contains("Behind the scenes clip"));

        String profileSettingsPublic = mockMvc.perform(get("/api/profile/" + ownerPublicId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(profileSettingsPublic.contains("reel-1.mp4"));
        assertTrue(profileSettingsPublic.contains("https://youtu.be/demo123"));
    }

    @Test
    void newlyJoinedAccountSeesExplicitPublicPostEvenIfAuthorPreviouslySetPhotoPostsPrivate() throws Exception {
        long suffix = System.nanoTime();
        String authorEmail = "pub.author." + suffix + "@truehunt.in";
        String authorToken = registerAndGetToken("Public Author", authorEmail, "9123456031");

        // Author previously turned photoPosts to private in Settings -> Visibility
        mockMvc.perform(patch("/api/settings/media-visibility")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoPosts\":\"private\"}"))
                .andExpect(status().isOk());

        // Author creates a brand-new photo post explicitly set to "public" in the composer
        MvcResult postRes = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Festival photo for everyone\",\"type\":\"post\",\"audience\":\"public\",\"media\":{\"url\":\"/uploads/festival.jpg\",\"kind\":\"image\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.audience").value("public"))
                .andReturn();
        String postId = objectMapper.readTree(postRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // A brand-new user joins after the post was published and sees it immediately
        String newJoinEmail = "new.join." + suffix + "@truehunt.in";
        String newJoinToken = registerAndGetToken("New Join User", newJoinEmail, "9123456032");
        String newJoinFeed = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + newJoinToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(newJoinFeed.contains("Festival photo for everyone"));

        // If author changes that post to private, the newly joined user no longer sees it
        mockMvc.perform(patch("/api/posts/" + postId)
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"audience\":\"private\"}"))
                .andExpect(status().isOk());
        String afterPrivateFeed = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + newJoinToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(afterPrivateFeed.contains("Festival photo for everyone"));
    }

    @Test
    void disablingLikeAndShareCountsStopsCountingLikesAndSharesUntilReEnabled() throws Exception {
        long suffix = System.nanoTime();
        String authorEmail = "eng.author." + suffix + "@truehunt.in";
        String viewerEmail = "eng.viewer." + suffix + "@truehunt.in";
        String authorToken = registerAndGetToken("Engagement Author", authorEmail, "9123456041");
        String viewerToken = registerAndGetToken("Engagement Viewer", viewerEmail, "9123456042");

        MvcResult postRes = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Engagement toggle test post\",\"type\":\"post\",\"audience\":\"public\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String postId = objectMapper.readTree(postRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Turn OFF Like counts and Share counts in Settings -> Likes & share counts
        mockMvc.perform(patch("/api/settings/engagement")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"likeCounts\":false,\"shareCounts\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.engagementPreferences.likeCounts").value(false))
                .andExpect(jsonPath("$.data.engagementPreferences.shareCounts").value(false));

        // Liking does NOT count while likeCounts is Off
        mockMvc.perform(post("/api/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likesCount").value(0));

        // Sharing is blocked and does NOT count while shareCounts is Off
        mockMvc.perform(post("/api/posts/" + postId + "/share")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sharesCount").value(0))
                .andExpect(jsonPath("$.data.shareCountDisabled").value(true))
                .andExpect(jsonPath("$.data.allowed").value(false));

        // GET /api/posts/{id} confirms 0 likes and 0 shares
        mockMvc.perform(get("/api/posts/" + postId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likesCount").value(0))
                .andExpect(jsonPath("$.data.sharesCount").value(0));

        // Turn ON Like counts and Share counts again -> likes and shares count normally
        mockMvc.perform(patch("/api/settings/engagement")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"likeCounts\":true,\"shareCounts\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likesCount").value(1));

        mockMvc.perform(post("/api/posts/" + postId + "/share")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sharesCount").value(1));
    }

    @Test
    void stoppingConnectingWithOthersInSettingsHidesUserFromSuggestionsBlocksRequestsAndNotificationsUntilReEnabled() throws Exception {
        long suffix = System.nanoTime();
        String userAEmail = "conn.a." + suffix + "@truehunt.in";
        String userBEmail = "conn.b." + suffix + "@truehunt.in";

        String userAToken = registerAndGetToken("Conn User A", userAEmail, "91234" + String.format("%05d", (suffix % 90000) + 10000));
        String userBToken = registerAndGetToken("Conn User B", userBEmail, "91234" + String.format("%05d", ((suffix + 1) % 90000) + 10000));

        // Get profiles to know public user IDs
        MvcResult resA = mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andReturn();
        String userAId = objectMapper.readTree(resA.getResponse().getContentAsString()).path("data").path("userId").asText();

        MvcResult resB = mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andReturn();
        String userBId = objectMapper.readTree(resB.getResponse().getContentAsString()).path("data").path("userId").asText();

        // Turn OFF "Connecting with others" for User A
        mockMvc.perform(patch("/api/settings/notifications")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"connections\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications.connections").value(false))
                .andExpect(jsonPath("$.data.notifications['connecting-with-others']").value(false));

        // User B tries to send a connection request to User A -> Blocked!
        mockMvc.perform(post("/api/connections/request")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + userAId + "\",\"message\":\"Hello\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("not accepting connection requests")));

        // User A tries to send a connection request to User B -> Blocked!
        mockMvc.perform(post("/api/connections/request")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + userBId + "\",\"message\":\"Hello\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("turned off connecting with others")));

        // User B gets suggestions -> User A is NOT shown to others
        String suggestionsForB = mockMvc.perform(get("/api/connections/suggestions")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(suggestionsForB.contains(userAId), "User A should not be shown in suggestions when connections are stopped");

        // User A gets suggestions -> returns empty list because User A stopped connecting with others
        mockMvc.perform(get("/api/connections/suggestions")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(0)));

        // User B views User A's profile -> connectionsCount is 0 / connections hidden
        mockMvc.perform(get("/api/profiles/" + userAId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connectionsCount").value(0))
                .andExpect(jsonPath("$.data.connectionsVisible").value(false));

        // Turn ON "Connecting with others" for User A
        mockMvc.perform(patch("/api/settings/notifications")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"connections\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications.connections").value(true));

        // Now User B sends connection request to User A -> Succeeds!
        mockMvc.perform(post("/api/connections/request")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + userAId + "\",\"message\":\"Hello again\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPending").value(true));

        // User A accepts connection from User B -> Succeeds!
        mockMvc.perform(post("/api/connections/accept")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + userBId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isConnected").value(true));
    }

    @Test
    void postAndOpportunityNotificationsReachConnectionsIndependentlyOfAuthorPreference() throws Exception {
        long suffix = System.nanoTime();
        String userAEmail = "postnotif.a." + suffix + "@truehunt.in";
        String userBEmail = "postnotif.b." + suffix + "@truehunt.in";
        String userCEmail = "postnotif.c." + suffix + "@truehunt.in";

        String userAToken = registerAndGetToken("Post Author A", userAEmail, "91234" + String.format("%05d", (suffix % 90000) + 10000));
        String userBToken = registerAndGetToken("Connected Friend B", userBEmail, "91234" + String.format("%05d", ((suffix + 1) % 90000) + 10000));
        String userCToken = registerAndGetToken("Stranger C", userCEmail, "91234" + String.format("%05d", ((suffix + 2) % 90000) + 10000));

        // Get profiles to get user IDs
        MvcResult resA = mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andReturn();
        String userAId = objectMapper.readTree(resA.getResponse().getContentAsString()).path("data").path("userId").asText();

        MvcResult resB = mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andReturn();
        String userBId = objectMapper.readTree(resB.getResponse().getContentAsString()).path("data").path("userId").asText();

        // Connect User A and User B
        mockMvc.perform(post("/api/connections/request")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + userBId + "\",\"message\":\"Let's connect\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/connections/accept")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + userAId + "\"}"))
                .andExpect(status().isOk());

        // 1. Initial State: "Posting and commenting" notifications are ON by default
        // User A creates post 1
        MvcResult post1Res = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Post 1 with notifications ON\",\"type\":\"post\",\"audience\":\"public\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String post1Id = objectMapper.readTree(post1Res.getResponse().getContentAsString()).path("data").path("id").asText();

        // Connected friend User B receives a notification
        String bNotifs1 = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(bNotifs1.contains("New Post") || bNotifs1.contains("Post Author A"), "Connected user B should receive notification for post 1");

        // Non-connected stranger User C does NOT receive any post notification
        String cNotifs1 = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userCToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(cNotifs1.contains("New Post from Post Author A"), "Stranger user C should not receive post notification");

        // User B comments on User A's post -> User A receives notification
        mockMvc.perform(post("/api/posts/" + post1Id + "/comments")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Great first post!\"}"))
                .andExpect(status().isOk());

        String aNotifs1 = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(aNotifs1.contains("New Comment"), "Post author A should receive comment notification from connection B");

        // 2. User A turns OFF "Posting and commenting" in Settings -> Notifications
        mockMvc.perform(patch("/api/settings/notifications")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"posting\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications.posting").value(false))
                .andExpect(jsonPath("$.data.notifications['posting-and-commenting']").value(false));

        // Get initial count of notifications for User B before post 2
        int bNotifsBeforeCount = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();

        // User A creates post 2 while notifications are stopped
        MvcResult post2Res = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Post 2 when notifications are STOPPED\",\"type\":\"post\",\"audience\":\"public\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String post2Id = objectMapper.readTree(post2Res.getResponse().getContentAsString()).path("data").path("id").asText();

        // Post 2 is visible to EVERYONE in feed!
        String feedForB = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(feedForB.contains("Post 2 when notifications are STOPPED"), "Post 2 must still show to everyone");

        String feedForC = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + userCToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(feedForC.contains("Post 2 when notifications are STOPPED"), "Post 2 must still show to everyone (including non-connections)");

        // The author's notification preference must not suppress alerts to their connections.
        String bNotifsAfterPost2 = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int bNotifsAfterPost2Count = objectMapper.readTree(bNotifsAfterPost2).path("data").size();
        assertEquals(bNotifsBeforeCount + 1, bNotifsAfterPost2Count,
                "Connected user B should receive one notification for post 2");
        assertTrue(bNotifsAfterPost2.contains("Post 2 when notifications are STOPPED"),
                "Connected user B should receive post notifications even if the author disabled their own notifications");

        mockMvc.perform(post("/api/opportunities")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Opportunity 2 while notifications are stopped\",\"content\":\"Casting call details\"}"))
                .andExpect(status().isOk());

        String bNotifsAfterOpportunity = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(bNotifsAfterOpportunity.contains("New Opportunity from Post Author A"),
                "The notification should identify the post as an opportunity");
        assertTrue(bNotifsAfterOpportunity.contains("Opportunity 2 while notifications are stopped"),
                "Connected user B should receive opportunity notifications too");

        // User A comments on post 1 while posting notifications are stopped -> comment is created but no notification to B
        int bNotifsBeforeComment = objectMapper.readTree(bNotifsAfterOpportunity).path("data").size();
        mockMvc.perform(post("/api/posts/" + post1Id + "/comments")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Author reply while notifications are disabled\"}"))
                .andExpect(status().isOk());

        int bNotifsAfterComment = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();
        assertEquals(bNotifsBeforeComment, bNotifsAfterComment, "No comment notification should be sent while author disabled posting notifications");

        // 3. User A turns ON "Posting and commenting" again
        mockMvc.perform(patch("/api/settings/notifications")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"posting\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications.posting").value(true));

        // User A creates post 3
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Post 3 with notifications resumed\",\"type\":\"post\",\"audience\":\"public\"}"))
                .andExpect(status().isOk());

        // User B receives notification for post 3
        int bNotifsFinalCount = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();
        assertTrue(bNotifsFinalCount > bNotifsBeforeComment, "User B should receive notification once notifications are re-enabled");
    }

    @Test
    void commentNotificationDeliveredToPostAuthorAndRemainingConnections() throws Exception {
        long suffix = System.nanoTime();
        String vamsiEmail = "vamsi." + suffix + "@truehunt.in";
        String friendAEmail = "friendA." + suffix + "@truehunt.in";
        String friendBEmail = "friendB." + suffix + "@truehunt.in";

        String vamsiToken = registerAndGetToken("Vamsi Tester", vamsiEmail, "91234" + String.format("%05d", (suffix % 80000) + 10000));
        String friendAToken = registerAndGetToken("Friend A", friendAEmail, "91234" + String.format("%05d", ((suffix + 1) % 80000) + 10000));
        String friendBToken = registerAndGetToken("Friend B", friendBEmail, "91234" + String.format("%05d", ((suffix + 2) % 80000) + 10000));

        // Get Vamsi, Friend A, Friend B public IDs
        String vamsiId = objectMapper.readTree(mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + vamsiToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("userId").asText();
        String friendAId = objectMapper.readTree(mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + friendAToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("userId").asText();
        String friendBId = objectMapper.readTree(mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + friendBToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("userId").asText();

        // Connect Vamsi with Friend A
        mockMvc.perform(post("/api/connections/request")
                        .header("Authorization", "Bearer " + vamsiToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + friendAId + "\",\"message\":\"Connect with Friend A\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/connections/accept")
                        .header("Authorization", "Bearer " + friendAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + vamsiId + "\"}"))
                .andExpect(status().isOk());

        // Connect Vamsi with Friend B
        mockMvc.perform(post("/api/connections/request")
                        .header("Authorization", "Bearer " + vamsiToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + friendBId + "\",\"message\":\"Connect with Friend B\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/connections/accept")
                        .header("Authorization", "Bearer " + friendBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + vamsiId + "\"}"))
                .andExpect(status().isOk());

        // Vamsi creates a post
        MvcResult vamsiPostRes = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + vamsiToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Vamsi's special update\",\"type\":\"post\",\"audience\":\"public\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String postId = objectMapper.readTree(vamsiPostRes.getResponse().getContentAsString()).path("data").path("id").asText();

        // 1. Friend A comments on Vamsi's post
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + friendAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Awesome post Vamsi!\"}"))
                .andExpect(status().isOk());

        // Verify Vamsi (Post Author) receives "New Comment on Your Post"
        String vamsiNotifs = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + vamsiToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(vamsiNotifs.contains("New Comment on Your Post"), "Vamsi should receive 'New Comment on Your Post'");
        assertTrue(vamsiNotifs.contains("Awesome post Vamsi!"), "Vamsi's notification should include comment text snippet");

        // Verify Friend B (Remaining Connection of Vamsi) receives "New Comment on Vamsi Tester's Post"
        String friendBNotifs = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + friendBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(friendBNotifs.contains("New Comment on Vamsi Tester's Post") || friendBNotifs.contains("Vamsi Tester"),
                "Friend B (remaining connection) should receive notification about comment on Vamsi's post");
        assertTrue(friendBNotifs.contains("Awesome post Vamsi!"), "Friend B's notification should include comment text snippet");

        // Verify Friend A (the commenter) does NOT receive comment notification for their own comment
        String friendANotifs = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + friendAToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(friendANotifs.contains("Awesome post Vamsi!"), "Friend A should not receive notification for their own comment");

        // 2. Friend A turns OFF posting and commenting notifications in Settings
        mockMvc.perform(patch("/api/settings/notifications")
                        .header("Authorization", "Bearer " + friendAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"posting\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications.posting").value(false));

        int vamsiNotifCountBefore = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + vamsiToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();
        int friendBNotifCountBefore = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + friendBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();

        // Friend A comments again while their posting notifications are disabled
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + friendAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Second comment while posting notifications disabled\"}"))
                .andExpect(status().isOk());

        // Verify NO new notifications sent to Vamsi or Friend B
        int vamsiNotifCountAfter = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + vamsiToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();
        int friendBNotifCountAfter = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + friendBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();

        assertEquals(vamsiNotifCountBefore, vamsiNotifCountAfter, "Vamsi should not receive comment notification when commenter has disabled posting notifications");
        assertEquals(friendBNotifCountBefore, friendBNotifCountAfter, "Friend B should not receive comment notification when commenter has disabled posting notifications");

        // 3. Friend A turns posting notifications back ON
        mockMvc.perform(patch("/api/settings/notifications")
                        .header("Authorization", "Bearer " + friendAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"posting\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications.posting").value(true));

        // 4. Vamsi (the Post Author) turns posting and commenting notifications OFF
        mockMvc.perform(patch("/api/settings/notifications")
                        .header("Authorization", "Bearer " + vamsiToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"posting\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications.posting").value(false));

        int vamsiNotifsBeforeAuthorOff = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + vamsiToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();
        int friendBNotifsBeforeAuthorOff = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + friendBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();

        // Friend A comments while Vamsi (Post Author) has posting notifications disabled
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + friendAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Third comment while post author disabled notifications\"}"))
                .andExpect(status().isOk());

        // Verify NO notifications sent to Vamsi or Friend B
        int vamsiNotifsAfterAuthorOff = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + vamsiToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();
        int friendBNotifsAfterAuthorOff = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + friendBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();

        assertEquals(vamsiNotifsBeforeAuthorOff, vamsiNotifsAfterAuthorOff, "Vamsi should not receive comment notifications when Vamsi stopped posting notifications");
        assertEquals(friendBNotifsBeforeAuthorOff, friendBNotifsAfterAuthorOff, "Friend B should not receive comment notifications when post author Vamsi stopped posting notifications");

        // 5. Vamsi turns posting notifications back ON
        mockMvc.perform(patch("/api/settings/notifications")
                        .header("Authorization", "Bearer " + vamsiToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"posting\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications.posting").value(true));

        // Friend A comments again after Vamsi re-enabled notifications
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + friendAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Fourth comment after notifications re-enabled!\"}"))
                .andExpect(status().isOk());

        int vamsiNotifsFinal = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + vamsiToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();
        int friendBNotifsFinal = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + friendBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();

        assertTrue(vamsiNotifsFinal > vamsiNotifsAfterAuthorOff, "Vamsi should receive notification once notifications are re-enabled");
        assertTrue(friendBNotifsFinal > friendBNotifsAfterAuthorOff, "Friend B should receive notification once notifications are re-enabled");
    }

    @Test
    void profileUpdatingNotificationSettingControlsNotificationsToConnections() throws Exception {
        long suffix = System.nanoTime();
        String vamsiEmail = "prof.vamsi." + suffix + "@truehunt.in";
        String friendAEmail = "prof.frienda." + suffix + "@truehunt.in";
        String friendBEmail = "prof.friendb." + suffix + "@truehunt.in";

        String vamsiToken = registerAndGetToken("Vamsi Alladi", vamsiEmail, "91234" + String.format("%05d", (suffix % 80000) + 10000));
        String friendAToken = registerAndGetToken("Friend A", friendAEmail, "91234" + String.format("%05d", ((suffix + 1) % 80000) + 10000));
        String friendBToken = registerAndGetToken("Friend B", friendBEmail, "91234" + String.format("%05d", ((suffix + 2) % 80000) + 10000));

        String vamsiId = objectMapper.readTree(mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + vamsiToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("userId").asText();
        String friendAId = objectMapper.readTree(mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + friendAToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("userId").asText();
        String friendBId = objectMapper.readTree(mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + friendBToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("userId").asText();

        // Connect Vamsi with Friend A
        mockMvc.perform(post("/api/connections/request")
                        .header("Authorization", "Bearer " + vamsiToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + friendAId + "\",\"message\":\"Connect with Friend A\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/connections/accept")
                        .header("Authorization", "Bearer " + friendAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + vamsiId + "\"}"))
                .andExpect(status().isOk());

        // Connect Vamsi with Friend B
        mockMvc.perform(post("/api/connections/request")
                        .header("Authorization", "Bearer " + vamsiToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + friendBId + "\",\"message\":\"Connect with Friend B\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/connections/accept")
                        .header("Authorization", "Bearer " + friendBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + vamsiId + "\"}"))
                .andExpect(status().isOk());

        int aNotifsInitial = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + friendAToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();
        int bNotifsInitial = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + friendBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();
        int vamsiNotifsInitial = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + vamsiToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();

        // 1. Updating profile while setting is ON (default)
        mockMvc.perform(patch("/api/profiles/me")
                        .header("Authorization", "Bearer " + vamsiToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"headline\":\"Award Winning Filmmaker\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headline").value("Award Winning Filmmaker"));

        JsonNode aNotifsStep1 = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + friendAToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data");
        JsonNode bNotifsStep1 = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + friendBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data");
        int vamsiNotifsStep1 = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + vamsiToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();

        assertEquals(vamsiNotifsInitial, vamsiNotifsStep1, "Vamsi should not receive a self-notification for updating his own profile");
        assertTrue(aNotifsStep1.size() > aNotifsInitial, "Friend A should receive a profile update notification");
        assertTrue(bNotifsStep1.size() > bNotifsInitial, "Friend B should receive a profile update notification");
        assertEquals("Profile Update", aNotifsStep1.get(0).path("title").asText());
        assertTrue(aNotifsStep1.get(0).path("message").asText().contains("Vamsi Alladi updated their profile"));

        // 2. Vamsi turns OFF "Updating your profile" in Settings
        mockMvc.perform(patch("/api/settings/notifications")
                        .header("Authorization", "Bearer " + vamsiToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profile\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications.profile").value(false));

        int aNotifsBeforeOff = aNotifsStep1.size();
        int bNotifsBeforeOff = bNotifsStep1.size();

        // Vamsi updates profile while setting is OFF
        mockMvc.perform(patch("/api/profiles/me")
                        .header("Authorization", "Bearer " + vamsiToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"headline\":\"Film Producer & Director\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headline").value("Film Producer & Director"));

        int aNotifsAfterOff = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + friendAToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();
        int bNotifsAfterOff = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + friendBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();

        assertEquals(aNotifsBeforeOff, aNotifsAfterOff, "Friend A should NOT receive profile update notification when Vamsi stopped it in settings");
        assertEquals(bNotifsBeforeOff, bNotifsAfterOff, "Friend B should NOT receive profile update notification when Vamsi stopped it in settings");

        // 3. Vamsi turns "profile" setting back ON, but Friend B turns it OFF for themselves
        mockMvc.perform(patch("/api/settings/notifications")
                        .header("Authorization", "Bearer " + vamsiToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profile\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications.profile").value(true));

        mockMvc.perform(patch("/api/settings/notifications")
                        .header("Authorization", "Bearer " + friendBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profile\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications.profile").value(false));

        int bNotifsBeforeStep3 = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + friendBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();

        // Vamsi updates profile again
        mockMvc.perform(patch("/api/profiles/me")
                        .header("Authorization", "Bearer " + vamsiToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"headline\":\"Studio Head\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headline").value("Studio Head"));

        int aNotifsFinal = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + friendAToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();
        int bNotifsFinal = objectMapper.readTree(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + friendBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").size();

        assertTrue(aNotifsFinal > aNotifsAfterOff, "Friend A should receive profile update notification");
        assertEquals(bNotifsBeforeStep3, bNotifsFinal, "Friend B should NOT receive notification because Friend B turned off profile update notifications");
    }

    @Test
    void publicProfilePrivacySettingControlsProfileVisibilityToConnectionsAndMutualConnections() throws Exception {
        long suffix = System.nanoTime();
        String userAEmail = "priv.vamsi." + suffix + "@truehunt.in";
        String userBEmail = "priv.friendb." + suffix + "@truehunt.in";
        String userCEmail = "priv.friendc." + suffix + "@truehunt.in";
        String userDEmail = "priv.stranger." + suffix + "@truehunt.in";

        String userAToken = registerAndGetToken("Vamsi Privacy", userAEmail, "91334" + String.format("%05d", (suffix % 80000) + 10000));
        String userBToken = registerAndGetToken("Direct Friend", userBEmail, "91334" + String.format("%05d", ((suffix + 1) % 80000) + 10000));
        String userCToken = registerAndGetToken("Mutual Friend", userCEmail, "91334" + String.format("%05d", ((suffix + 2) % 80000) + 10000));
        String userDToken = registerAndGetToken("Stranger User", userDEmail, "91334" + String.format("%05d", ((suffix + 3) % 80000) + 10000));

        String userAId = objectMapper.readTree(mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("userId").asText();
        String userBId = objectMapper.readTree(mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("userId").asText();
        String userCId = objectMapper.readTree(mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + userCToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("userId").asText();
        String userDId = objectMapper.readTree(mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + userDToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("userId").asText();

        // 1. Connect User A with User B (Direct Connection)
        mockMvc.perform(post("/api/connections/request")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + userBId + "\",\"message\":\"Connect with Friend B\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/connections/accept")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + userAId + "\"}"))
                .andExpect(status().isOk());

        // 2. Connect User B with User C (Making C a Mutual Connection of A through B)
        mockMvc.perform(post("/api/connections/request")
                        .header("Authorization", "Bearer " + userCToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + userBId + "\",\"message\":\"Connect C with B\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/connections/accept")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + userCId + "\"}"))
                .andExpect(status().isOk());

        // 3. User A has "Public profile" ON by default:
        // Everyone (Stranger D, Friend B, Mutual C, and A) can view A's profile
        mockMvc.perform(get("/api/profiles/" + userAId)
                        .header("Authorization", "Bearer " + userDToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Vamsi Privacy"));

        mockMvc.perform(get("/api/profiles/" + userAId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isConnected").value(true));

        mockMvc.perform(get("/api/profiles/" + userAId)
                        .header("Authorization", "Bearer " + userCToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isMutualConnection").value(true));

        mockMvc.perform(get("/api/profiles/" + userAId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isOwner").value(true));

        // 4. User A turns "Public profile" (profileVisibility) OFF in Settings -> Privacy
        mockMvc.perform(patch("/api/settings/privacy")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileVisibility\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.privacyPreferences.profileVisibility").value(false));

        // 5. Verify restricted visibility when Public profile is OFF:
        // Stranger User D is BLOCKED (403 Forbidden)
        mockMvc.perform(get("/api/profiles/" + userAId)
                        .header("Authorization", "Bearer " + userDToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("This profile is private. Only connections and mutual connections can view this profile."));

        // Anonymous/Public viewer is BLOCKED (403 Forbidden)
        mockMvc.perform(get("/api/profiles/" + userAId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // Direct Connection User B CAN VIEW (200 OK)
        mockMvc.perform(get("/api/profiles/" + userAId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Vamsi Privacy"))
                .andExpect(jsonPath("$.data.isConnected").value(true));

        // Mutual Connection User C CAN VIEW (200 OK)
        mockMvc.perform(get("/api/profiles/" + userAId)
                        .header("Authorization", "Bearer " + userCToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Vamsi Privacy"))
                .andExpect(jsonPath("$.data.isMutualConnection").value(true));

        // Profile Owner User A CAN VIEW (200 OK)
        mockMvc.perform(get("/api/profiles/" + userAId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Vamsi Privacy"))
                .andExpect(jsonPath("$.data.isOwner").value(true));

        // 6. User A turns "Public profile" back ON
        mockMvc.perform(patch("/api/settings/privacy")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileVisibility\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.privacyPreferences.profileVisibility").value(true));

        // Stranger D can now view A's profile again (200 OK)
        mockMvc.perform(get("/api/profiles/" + userAId)
                        .header("Authorization", "Bearer " + userDToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Vamsi Privacy"));
    }

    @Test
    void connectionsCountAndConnectedProfilesListMatchExactlyWhenMultipleConnectionsExist() throws Exception {
        long seed = System.nanoTime();
        String userAEmail = "conn.main." + seed + "@truehunt.in";
        String userBEmail = "conn.friend1." + seed + "@truehunt.in";
        String userCEmail = "conn.friend2." + seed + "@truehunt.in";

        String tokenA = registerAndGetToken("User A", userAEmail, "9777" + (seed % 1000000));
        String tokenB = registerAndGetToken("User B", userBEmail, "9778" + (seed % 1000000));
        String tokenC = registerAndGetToken("User C", userCEmail, "9779" + (seed % 1000000));

        // Get public user IDs
        MvcResult resA = mockMvc.perform(get("/api/profiles/me").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andReturn();
        String idA = objectMapper.readTree(resA.getResponse().getContentAsString()).path("data").path("userId").asText();

        MvcResult resB = mockMvc.perform(get("/api/profiles/me").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk()).andReturn();
        String idB = objectMapper.readTree(resB.getResponse().getContentAsString()).path("data").path("userId").asText();

        MvcResult resC = mockMvc.perform(get("/api/profiles/me").header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isOk()).andReturn();
        String idC = objectMapper.readTree(resC.getResponse().getContentAsString()).path("data").path("userId").asText();

        // 1. Connect User A and User B
        mockMvc.perform(post("/api/connections/request")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + idB + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/connections/accept")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + idA + "\"}"))
                .andExpect(status().isOk());

        // 2. Connect User A and User C
        mockMvc.perform(post("/api/connections/request")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + idC + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/connections/accept")
                        .header("Authorization", "Bearer " + tokenC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":\"" + idA + "\"}"))
                .andExpect(status().isOk());

        // 3. User A profile connectionsCount must be exactly 2
        mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connectionsCount").value(2));

        // 4. User A getConnectedProfiles must return exactly 2 connections
        MvcResult connListRes = mockMvc.perform(get("/api/connections")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn();

        JsonNode connList = objectMapper.readTree(connListRes.getResponse().getContentAsString()).path("data");
        List<String> returnedNames = List.of(connList.get(0).path("name").asText(), connList.get(1).path("name").asText());
        assertTrue(returnedNames.contains("User B"), "List must contain User B");
        assertTrue(returnedNames.contains("User C"), "List must contain User C");

        // 5. User B turns off "connections" notifications in Settings -> Notifications
        mockMvc.perform(patch("/api/settings/notifications")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"connections\":false,\"connecting-with-others\":false}"))
                .andExpect(status().isOk());

        // 6. User A still sees both 2 connections in profile and in /api/connections
        mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connectionsCount").value(2));

        mockMvc.perform(get("/api/connections")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
