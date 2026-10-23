package com.truehunt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truehunt.dto.request.*;
import com.truehunt.model.MediaAttachment;
import com.truehunt.model.Notification;
import com.truehunt.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EdgeCasesTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationService notificationService;

    private static String userAToken;
    private static String userAId;

    private static String userBToken;
    private static String userBId;

    private static String userCToken;
    private static String userCId;

    private static boolean initialized = false;

    @BeforeEach
    public void ensureUsersSetup() throws Exception {
        if (initialized) {
            return;
        }

        // Register User A (Alice Artist)
        RegisterRequest reqA = new RegisterRequest();
        reqA.setEmail("alice_edge@truehunt.com");
        reqA.setPassword("password123");
        reqA.setName("Alice Artist");
        MvcResult resA = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isCreated())
                .andReturn();
        userAToken = objectMapper.readTree(resA.getResponse().getContentAsString()).path("data").path("token").asText();
        userAId = objectMapper.readTree(resA.getResponse().getContentAsString()).path("data").path("user").path("id").asText();

        // Update Alice's contact info
        UpdateProfileRequest updateA = new UpdateProfileRequest();
        updateA.setPhone("+1234567890");
        updateA.setEmail("alice.contact@truehunt.com");
        updateA.setAddress("123 Melody Lane, Music City");
        mockMvc.perform(patch("/api/profiles/me")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateA)))
                .andExpect(status().isOk());

        // Register User B (Bob Producer)
        RegisterRequest reqB = new RegisterRequest();
        reqB.setEmail("bob_edge@truehunt.com");
        reqB.setPassword("password123");
        reqB.setName("Bob Producer");
        MvcResult resB = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isCreated())
                .andReturn();
        userBToken = objectMapper.readTree(resB.getResponse().getContentAsString()).path("data").path("token").asText();
        userBId = objectMapper.readTree(resB.getResponse().getContentAsString()).path("data").path("user").path("id").asText();

        // Register User C (Charlie Stranger)
        RegisterRequest reqC = new RegisterRequest();
        reqC.setEmail("charlie_edge@truehunt.com");
        reqC.setPassword("password123");
        reqC.setName("Charlie Stranger");
        MvcResult resC = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqC)))
                .andExpect(status().isCreated())
                .andReturn();
        userCToken = objectMapper.readTree(resC.getResponse().getContentAsString()).path("data").path("token").asText();
        userCId = objectMapper.readTree(resC.getResponse().getContentAsString()).path("data").path("user").path("id").asText();

        initialized = true;
    }

    // ==========================================
    // 1. AUTHENTICATION & SECURITY EDGE CASES
    // ==========================================
    @Test
    @DisplayName("Register with invalid email format returns 400 VALIDATION_ERROR")
    public void testAuth_RegisterInvalidEmail() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("not-an-email-address");
        req.setPassword("validPass123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.email").isNotEmpty());
    }

    @Test
    @DisplayName("Register with password shorter than 6 characters returns 400 VALIDATION_ERROR")
    public void testAuth_RegisterShortPassword() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("shortpass@truehunt.com");
        req.setPassword("12345");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.password").isNotEmpty());
    }

    @Test
    @DisplayName("Login with wrong password returns 401 UNAUTHENTICATED")
    public void testAuth_LoginWrongPassword() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice_edge@truehunt.com");
        req.setPassword("completelyWrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Login with non-existent email returns 401 UNAUTHENTICATED")
    public void testAuth_LoginNonExistentEmail() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("ghost_user@truehunt.com");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Accessing protected GET /api/auth/me without token returns 401 UNAUTHENTICATED")
    public void testAuth_GetMeWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Accessing protected endpoint with invalid/malformed token returns 401 UNAUTHENTICATED")
    public void testAuth_AccessWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer invalid-tampered-token-xyz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Logout clears token cookie")
    public void testAuth_LogoutClearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("token"))
                .andExpect(cookie().maxAge("token", 0));
    }

    // ==========================================
    // 2. PROFILE & CONTACT PRIVACY EDGE CASES
    // ==========================================
    @Test
    @DisplayName("User views own profile: contact details ARE visible")
    public void testProfile_OwnContactDetailsVisible() throws Exception {
        mockMvc.perform(get("/api/profiles/" + userAId)
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isOwnProfile").value(true))
                .andExpect(jsonPath("$.data.phone").value("+1234567890"))
                .andExpect(jsonPath("$.data.email").value("alice.contact@truehunt.com"))
                .andExpect(jsonPath("$.data.address").value("123 Melody Lane, Music City"));
    }

    @Test
    @DisplayName("Unconnected stranger (User C) views User A: contact details are REDACTED (null)")
    public void testProfile_StrangerViewsProfileContactRedacted() throws Exception {
        mockMvc.perform(get("/api/profiles/" + userAId)
                .header("Authorization", "Bearer " + userCToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isOwnProfile").value(false))
                .andExpect(jsonPath("$.data.isConnected").value(false))
                .andExpect(jsonPath("$.data.phone").doesNotExist())
                .andExpect(jsonPath("$.data.address").doesNotExist());
    }

    @Test
    @DisplayName("Viewing non-existent user profile returns 404 NOT_FOUND")
    public void testProfile_ViewNonExistentProfile() throws Exception {
        mockMvc.perform(get("/api/profiles/00000000-0000-0000-0000-000000000000")
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Private profile cannot be viewed by unconnected user: returns 403 FORBIDDEN")
    public void testProfile_PrivateProfileHiddenFromStrangers() throws Exception {
        // User C sets visibility to private
        UpdateProfileRequest updateC = new UpdateProfileRequest();
        updateC.setVisibility("private");
        mockMvc.perform(patch("/api/profiles/me")
                .header("Authorization", "Bearer " + userCToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateC)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibility").value("private"));

        // User A tries to view User C's private profile
        mockMvc.perform(get("/api/profiles/" + userCId)
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        // Restore User C back to public
        updateC.setVisibility("public");
        mockMvc.perform(patch("/api/profiles/me")
                .header("Authorization", "Bearer " + userCToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateC)))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 3. MEDIA UPLOAD & SERVING EDGE CASES
    // ==========================================
    @Test
    @DisplayName("Upload valid profile image: returns 201 with objectKey and accessible URL")
    public void testMedia_UploadValidImage() throws Exception {
        MockMultipartFile validImage = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "fake-png-image-content".getBytes()
        );

        MvcResult res = mockMvc.perform(multipart("/api/media/upload")
                .file(validImage)
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.objectKey").isNotEmpty())
                .andExpect(jsonPath("$.data.url").isNotEmpty())
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andReturn();

        String objectKey = objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("objectKey").asText();

        // Verify file can be served publicly
        mockMvc.perform(get("/api/media/" + objectKey))
                .andExpect(status().isOk())
                .andExpect(content().bytes("fake-png-image-content".getBytes()));
    }

    @Test
    @DisplayName("Upload demo video <= 5 MB: succeeds with 201 CREATED")
    public void testMedia_UploadDemoVideoUnder5MB() throws Exception {
        byte[] smallVideoBytes = new byte[1024 * 100]; // 100 KB
        MockMultipartFile videoFile = new MockMultipartFile(
                "file",
                "guitar_solo.mp4",
                "video/mp4",
                smallVideoBytes
        );

        mockMvc.perform(multipart("/api/media/upload")
                .file(videoFile)
                .param("category", "demo_video")
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.objectKey").isNotEmpty())
                .andExpect(jsonPath("$.data.fileName").value("guitar_solo.mp4"));
    }

    @Test
    @DisplayName("Server-side demo video limit enforcement: uploading video > 5 MB returns 400 VALIDATION_ERROR")
    public void testMedia_UploadDemoVideoOver5MBFails() throws Exception {
        // 5.5 MB demo video (5,767,168 bytes > 5,242,880 limit)
        byte[] largeVideoBytes = new byte[5 * 1024 * 1024 + 512 * 1024];
        MockMultipartFile oversizedVideo = new MockMultipartFile(
                "file",
                "large_rehearsal.mp4",
                "video/mp4",
                largeVideoBytes
        );

        mockMvc.perform(multipart("/api/media/upload")
                .file(oversizedVideo)
                .param("category", "demo_video")
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message", containsString("5 MB")));
    }

    @Test
    @DisplayName("Upload file with forbidden executable extension (.exe) returns 400 VALIDATION_ERROR")
    public void testMedia_UploadForbiddenExtension() throws Exception {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/octet-stream",
                "dangerous-payload".getBytes()
        );

        mockMvc.perform(multipart("/api/media/upload")
                .file(exeFile)
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message", containsString("Unsupported file type")));
    }

    @Test
    @DisplayName("Upload empty file returns 400 VALIDATION_ERROR")
    public void testMedia_UploadEmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/media/upload")
                .file(emptyFile)
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Request non-existent media resource returns 404 NOT_FOUND")
    public void testMedia_ServeNonExistentMedia() throws Exception {
        mockMvc.perform(get("/api/media/non-existent-file-key.png"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // ==========================================
    // 4. POSTS & OPPORTUNITIES EDGE CASES
    // ==========================================
    @Test
    @DisplayName("Create post with invalid type returns 400 VALIDATION_ERROR")
    public void testPost_CreatePostInvalidType() throws Exception {
        CreatePostRequest req = new CreatePostRequest();
        req.setType("unsupported_type_xyz");
        req.setContent("Content here");

        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Create post with blank content returns 400 VALIDATION_ERROR")
    public void testPost_CreatePostBlankContent() throws Exception {
        CreatePostRequest req = new CreatePostRequest();
        req.setType("post");
        req.setContent("   ");

        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.content").isNotEmpty());
    }

    @Test
    @DisplayName("Non-author cannot attach media to someone else's post: returns 403 FORBIDDEN")
    public void testPost_NonAuthorCannotAttachMedia() throws Exception {
        // User A creates post
        CreatePostRequest postReq = new CreatePostRequest();
        postReq.setType("post");
        postReq.setContent("User A original track snippet");
        MvcResult res = mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String postId = objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // User B attempts to attach media to User A's post
        MediaAttachment attachment = new MediaAttachment("key123", "http://url", "cover.png", "image/png", 500L);
        mockMvc.perform(post("/api/posts/" + postId + "/media")
                .header("Authorization", "Bearer " + userBToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(attachment)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Attach media to non-existent post returns 404 NOT_FOUND")
    public void testPost_AttachMediaToNonExistentPost() throws Exception {
        MediaAttachment attachment = new MediaAttachment("key123", "http://url", "cover.png", "image/png", 500L);
        mockMvc.perform(post("/api/posts/00000000-0000-0000-0000-000000000000/media")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(attachment)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Create opportunity with blank content returns 400 VALIDATION_ERROR")
    public void testPost_CreateOpportunityBlankContent() throws Exception {
        CreateOpportunityRequest opp = new CreateOpportunityRequest();
        opp.setTitle("Bassist Wanted");
        opp.setContent("   ");

        mockMvc.perform(post("/api/opportunities")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(opp)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    // ==========================================
    // 5. CONNECTIONS & NETWORKING EDGE CASES
    // ==========================================
    @Test
    @DisplayName("Connecting to oneself returns 400 VALIDATION_ERROR")
    public void testConnection_SelfConnectionFails() throws Exception {
        ConnectionRequestDto selfReq = new ConnectionRequestDto(userAId);

        mockMvc.perform(post("/api/connections")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(selfReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message", containsString("cannot connect with yourself")));
    }

    @Test
    @DisplayName("Connecting to non-existent user returns 404 NOT_FOUND")
    public void testConnection_ConnectionToNonExistentUser() throws Exception {
        ConnectionRequestDto req = new ConnectionRequestDto("00000000-0000-0000-0000-000000000000");

        mockMvc.perform(post("/api/connections")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Duplicate connection request while already pending returns 409 CONFLICT")
    public void testConnection_DuplicatePendingConnection() throws Exception {
        // User C requests User B
        ConnectionRequestDto req = new ConnectionRequestDto(userBId);
        mockMvc.perform(post("/api/connections")
                .header("Authorization", "Bearer " + userCToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // User C requests User B again immediately -> 409 Conflict
        mockMvc.perform(post("/api/connections")
                .header("Authorization", "Bearer " + userCToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message", containsString("already pending")));
    }

    @Test
    @DisplayName("Requester (User A) or third-party (User C) cannot accept connection: returns 403 FORBIDDEN")
    public void testConnection_OnlyRecipientCanRespondToConnection() throws Exception {
        // User A requests User B
        ConnectionRequestDto req = new ConnectionRequestDto(userBId);
        MvcResult reqRes = mockMvc.perform(post("/api/connections")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String connId = objectMapper.readTree(reqRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        ConnectionActionRequest acceptReq = new ConnectionActionRequest("accepted");

        // Requester (User A) tries to accept own request -> 403 Forbidden
        mockMvc.perform(patch("/api/connections/" + connId)
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        // Stranger (User C) tries to accept -> 403 Forbidden
        mockMvc.perform(patch("/api/connections/" + connId)
                .header("Authorization", "Bearer " + userCToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        // Valid recipient (User B) accepts -> 200 OK
        mockMvc.perform(patch("/api/connections/" + connId)
                .header("Authorization", "Bearer " + userBToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("accepted"));

        // Now that User A and User B are connected, User B views User A: contact details ARE visible!
        mockMvc.perform(get("/api/profiles/" + userAId)
                .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isConnected").value(true))
                .andExpect(jsonPath("$.data.phone").value("+1234567890"))
                .andExpect(jsonPath("$.data.address").value("123 Melody Lane, Music City"));
    }

    // ==========================================
    // 6. MESSAGING & CONVERSATIONS EDGE CASES
    // ==========================================
    @Test
    @DisplayName("Cannot start direct conversation with oneself: returns 400 VALIDATION_ERROR")
    public void testMessaging_DirectConversationWithSelfFails() throws Exception {
        mockMvc.perform(post("/api/conversations/direct/" + userAId)
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Non-participant cannot read conversation messages: returns 403 FORBIDDEN")
    public void testMessaging_NonParticipantCannotReadMessages() throws Exception {
        // Create conversation between User A and User B
        MvcResult convRes = mockMvc.perform(post("/api/conversations/direct/" + userBId)
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andReturn();
        String convId = objectMapper.readTree(convRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Stranger (User C) attempts to view messages
        mockMvc.perform(get("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + userCToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Non-participant cannot send message in conversation: returns 403 FORBIDDEN")
    public void testMessaging_NonParticipantCannotSendMessage() throws Exception {
        // Conversation between A and B
        MvcResult convRes = mockMvc.perform(post("/api/conversations/direct/" + userBId)
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andReturn();
        String convId = objectMapper.readTree(convRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // User C attempts to send message in A & B conversation
        SendMessageRequest msgReq = new SendMessageRequest();
        msgReq.setBody("I am intruding into your chat!");

        mockMvc.perform(post("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + userCToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Sending empty message body returns 400 VALIDATION_ERROR")
    public void testMessaging_SendEmptyMessageBodyFails() throws Exception {
        MvcResult convRes = mockMvc.perform(post("/api/conversations/direct/" + userBId)
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andReturn();
        String convId = objectMapper.readTree(convRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        SendMessageRequest emptyMsg = new SendMessageRequest();
        emptyMsg.setBody("   ");

        mockMvc.perform(post("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyMsg)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    // ==========================================
    // 7. NOTIFICATIONS EDGE CASES
    // ==========================================
    @Test
    @DisplayName("User cannot mark someone else's notification as read: returns 404 NOT_FOUND")
    public void testNotification_CannotMarkOtherUsersNotificationRead() throws Exception {
        // Explicitly create a notification for User B
        Notification createdNotif = notificationService.createNotification(
                userBId, userAId, "connection_request", "connection", "dummy-resource-id"
        );
        String notifId = createdNotif.getId();

        // User A attempts to mark User B's notification read -> 404 NOT_FOUND
        mockMvc.perform(patch("/api/notifications/" + notifId + "/read")
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

        // User B marks own notification read -> 200 OK
        mockMvc.perform(patch("/api/notifications/" + notifId + "/read")
                .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());
    }

    @Test
    @DisplayName("Bulk read with specific IDs marks only specified notifications")
    public void testNotification_BulkReadSpecificIds() throws Exception {
        BulkReadNotificationsRequest bulkReq = new BulkReadNotificationsRequest();
        bulkReq.setNotificationIds(Collections.singletonList("00000000-0000-0000-0000-000000000000"));

        mockMvc.perform(patch("/api/notifications/read")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bulkReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markedReadCount").value(0));
    }
}
