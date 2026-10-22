package com.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.dto.request.LoginRequest;
import com.social.dto.request.RegisterRequest;
import com.social.dto.response.AuthResponse;
import com.social.entity.Profile;
import com.social.entity.User;
import com.social.entity.UserStatus;
import com.social.entity.UserSession;
import com.social.repository.ProfileRepository;
import com.social.repository.UserRepository;
import com.social.repository.UserSessionRepository;
import com.social.util.IdGenerator;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        sessionRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should register user successfully and create empty profile and session cookie")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("  Alice.In.Wonderland@Example.COM  ")
                .password("SecretPassword123!")
                .displayName("Alice")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("session_token"))
                .andExpect(cookie().httpOnly("session_token", true))
                .andExpect(jsonPath("$.data.user.id").isNotEmpty())
                .andExpect(jsonPath("$.data.user._id").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("alice.in.wonderland@example.com"))
                .andExpect(jsonPath("$.data.user.status").value("active"))
                .andExpect(jsonPath("$.data.profile.id").isNotEmpty())
                .andExpect(jsonPath("$.data.profile._id").isNotEmpty())
                .andExpect(jsonPath("$.data.profile.displayName").value("Alice"))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andReturn();

        // Verify entities in database
        User savedUser = userRepository.findByEmail("alice.in.wonderland@example.com").orElse(null);
        assertNotNull(savedUser);
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
        assertTrue(passwordEncoder.matches("SecretPassword123!", savedUser.getPasswordHash()));

        Profile savedProfile = profileRepository.findByUserId(savedUser.getId()).orElse(null);
        assertNotNull(savedProfile);
        assertEquals("Alice", savedProfile.getDisplayName());
    }

    @Test
    @DisplayName("Should enforce password pattern rules (uppercase, lowercase, number, special char, no spaces)")
    void testPasswordPatternValidationFailures() throws Exception {
        // Missing uppercase
        RegisterRequest noUpper = RegisterRequest.builder()
                .email("test.noupper@example.com")
                .password("password123!")
                .build();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noUpper)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.password").value(containsString("Password must contain uppercase, lowercase, a number, and a special character, with no spaces")));

        // Missing lowercase
        RegisterRequest noLower = RegisterRequest.builder()
                .email("test.nolower@example.com")
                .password("PASSWORD123!")
                .build();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noLower)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        // Missing number
        RegisterRequest noDigit = RegisterRequest.builder()
                .email("test.nodigit@example.com")
                .password("Password!")
                .build();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noDigit)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        // Missing special character
        RegisterRequest noSpecial = RegisterRequest.builder()
                .email("test.nospecial@example.com")
                .password("Password123")
                .build();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noSpecial)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        // Contains spaces
        RegisterRequest withSpaces = RegisterRequest.builder()
                .email("test.spaces@example.com")
                .password("Password 123!")
                .build();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withSpaces)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Should reject registration with duplicate email with 409 Conflict")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequest first = RegisterRequest.builder()
                .email("duplicate@example.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest duplicate = RegisterRequest.builder()
                .email("DUPLICATE@EXAMPLE.COM")
                .password("DifferentPassword456!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value(containsString("already in use")));
    }

    @Test
    @DisplayName("Should reject invalid email and short password with 400 Bad Request")
    void testRegisterValidationFailures() throws Exception {
        RegisterRequest invalidEmail = RegisterRequest.builder()
                .email("not-a-valid-email")
                .password("ValidPass123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        RegisterRequest shortPassword = RegisterRequest.builder()
                .email("valid@example.com")
                .password("123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Should log in successfully with valid credentials and return session cookie")
    void testLoginSuccess() throws Exception {
        // Register user
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("login.test@example.com")
                .password("SecureSecret123!")
                .displayName("Login Tester")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Perform login
        LoginRequest loginReq = LoginRequest.builder()
                .email("LOGIN.TEST@EXAMPLE.COM")
                .password("SecureSecret123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "192.168.1.4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("session_token"))
                .andExpect(cookie().httpOnly("session_token", true))
                .andExpect(jsonPath("$.data.user.email").value("login.test@example.com"))
                .andExpect(jsonPath("$.data.profile.displayName").value("Login Tester"))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Should return generic error message for invalid password or non-existent email")
    void testLoginGenericErrorForBadCredentials() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("cred.test@example.com")
                .password("CorrectPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Wrong password
        LoginRequest wrongPass = LoginRequest.builder()
                .email("cred.test@example.com")
                .password("WrongPassword999!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "192.168.1.5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPass)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message").value("Invalid email or password"));

        // Non-existent email
        LoginRequest nonExistent = LoginRequest.builder()
                .email("nobody@example.com")
                .password("AnyPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "192.168.1.5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistent)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Suspended or deleted users cannot sign in")
    void testSuspendedAndDeletedUsersCannotSignIn() throws Exception {
        // Create suspended user
        String suspendedId = IdGenerator.nextId();
        User suspendedUser = User.builder()
                .id(suspendedId)
                .email("suspended@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.SUSPENDED)
                .build();
        userRepository.save(suspendedUser);

        LoginRequest suspendedLogin = LoginRequest.builder()
                .email("suspended@example.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "192.168.1.6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suspendedLogin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value(containsString("suspended")));

        // Create deleted user
        String deletedId = IdGenerator.nextId();
        User deletedUser = User.builder()
                .id(deletedId)
                .email("deleted@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.DELETED)
                .build();
        userRepository.save(deletedUser);

        LoginRequest deletedLogin = LoginRequest.builder()
                .email("deleted@example.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "192.168.1.6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deletedLogin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value(containsString("deleted")));
    }

    @Test
    @DisplayName("Should identify caller via HttpOnly session cookie on GET /api/auth/me")
    void testMeEndpointWithSessionCookie() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("cookie.user@example.com")
                .password("SecretPassword123!")
                .displayName("Cookie Monster")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        jakarta.servlet.http.Cookie sessionCookie = registerResult.getResponse().getCookie("session_token");
        assertNotNull(sessionCookie);

        mockMvc.perform(get("/api/auth/me")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("cookie.user@example.com"))
                .andExpect(jsonPath("$.data.user.status").value("active"))
                .andExpect(jsonPath("$.data.profile.displayName").value("Cookie Monster"))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Should identify caller via Authorization Bearer session token on GET /api/auth/me")
    void testMeEndpointWithSessionBearerToken() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("bearer.user@example.com")
                .password("SecretPassword123!")
                .displayName("Bearer User")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        jakarta.servlet.http.Cookie sessionCookie = registerResult.getResponse().getCookie("session_token");
        assertNotNull(sessionCookie);
        String sessionToken = sessionCookie.getValue();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + sessionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("bearer.user@example.com"))
                .andExpect(jsonPath("$.data.profile.displayName").value("Bearer User"));
    }

    @Test
    @DisplayName("Should identify caller via X-Session-Id header on GET /api/auth/me")
    void testMeEndpointWithSessionIdHeader() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("session.header.user@example.com")
                .password("SecretPassword123!")
                .displayName("Session Header User")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.88")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        jakarta.servlet.http.Cookie sessionCookie = registerResult.getResponse().getCookie("session_token");
        assertNotNull(sessionCookie);
        String sessionId = sessionCookie.getValue();

        mockMvc.perform(get("/api/auth/me")
                        .header("X-Session-Id", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("session.header.user@example.com"))
                .andExpect(jsonPath("$.data.profile.displayName").value("Session Header User"));
    }

    @Test
    @DisplayName("Should reject unauthenticated calls to GET /api/auth/me with 401 Unauthorized")
    void testMeEndpointUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("NOT_AUTHENTICATED"));
    }

    @Test
    @DisplayName("Should reject malformed or invalid Bearer tokens with 401 Unauthorized")
    void testMeEndpointWithMalformedBearerToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid-bearer-token-123456"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("NOT_AUTHENTICATED"));
    }

    @Test
    @DisplayName("Should logout, terminate session, clear cookie, and subsequent requests should fail")
    void testLogoutEndsSession() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("logout.user@example.com")
                .password("SecretPassword123!")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        jakarta.servlet.http.Cookie sessionCookie = registerResult.getResponse().getCookie("session_token");
        assertNotNull(sessionCookie);
        String token = sessionCookie.getValue();

        // Ensure session exists
        assertTrue(sessionRepository.findByToken(token).isPresent());

        // Perform logout
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("session_token", 0))
                .andExpect(jsonPath("$.data.message").value("Logged out successfully"));

        // Verify session was removed from database
        assertTrue(sessionRepository.findByToken(token).isEmpty());

        // Subsequent call to /api/auth/me with old token/cookie must fail
        mockMvc.perform(get("/api/auth/me")
                        .cookie(sessionCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should rate limit excessive login attempts and return 429 Too Many Requests")
    void testLoginRateLimiting() throws Exception {
        LoginRequest loginReq = LoginRequest.builder()
                .email("ratelimit@example.com")
                .password("WrongPass123!")
                .build();

        String testIp = "10.99.88.77";

        // Exceed max-requests (configured as 10 in application.properties)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", testIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isUnauthorized());
        }

        // 11th request must be rejected with 429
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.error.message").value(containsString("Rate limit exceeded")));
    }

    @Test
    @DisplayName("Should rate limit excessive registration attempts and return 429 Too Many Requests")
    void testRegisterRateLimiting() throws Exception {
        String testIp = "10.99.88.99";

        // Exceed max-requests (10 in application.properties)
        for (int i = 0; i < 10; i++) {
            RegisterRequest regReq = RegisterRequest.builder()
                    .email("ratereg" + i + "@example.com")
                    .password("Password123!")
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .header("X-Forwarded-For", testIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(regReq)))
                    .andExpect(status().isCreated());
        }

        // 11th registration attempt must be rejected with 429
        RegisterRequest excessReq = RegisterRequest.builder()
                .email("excess@example.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessReq)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.error.message").value(containsString("Rate limit exceeded")));
    }

    @Test
    @DisplayName("Should login successfully with whitespace and mixed-case email")
    void testLoginWithWhitespaceAndMixedCaseEmail() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("space.mixed@example.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = LoginRequest.builder()
                .email("   SPACE.MIXED@EXAMPLE.COM   ")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "192.168.1.50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("space.mixed@example.com"));
    }

    @Test
    @DisplayName("Should reject expired session token with 401 Unauthorized")
    void testExpiredSessionTokenRejected() throws Exception {
        // Create user
        String userId = IdGenerator.nextId();
        User user = User.builder()
                .id(userId)
                .email("expired@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        // Create expired session
        String expiredToken = "expired-token-123456";
        UserSession session = new UserSession(
                IdGenerator.nextId(),
                expiredToken,
                userId,
                Instant.now().minusSeconds(3600), // Expired 1 hour ago
                Instant.now().minusSeconds(7200)
        );
        sessionRepository.save(session);

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie("session_token", expiredToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("NOT_AUTHENTICATED"));

        mockMvc.perform(get("/api/auth/me")
                        .header("X-Session-Id", expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("NOT_AUTHENTICATED"));
    }

    @Test
    @DisplayName("Should block user who became suspended or deleted mid-session with 403 Forbidden")
    void testUserStatusChangeMidSession() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("midsession@example.com")
                .password("Password123!")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.60")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie sessionCookie = result.getResponse().getCookie("session_token");
        assertNotNull(sessionCookie);
        String sessionToken = sessionCookie.getValue();

        // Verify active session works with both Cookie and Bearer session token
        mockMvc.perform(get("/api/auth/me").cookie(sessionCookie))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + sessionToken))
                .andExpect(status().isOk());

        // Update user status to SUSPENDED
        User user = userRepository.findByEmail("midsession@example.com").orElseThrow();
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        // Attempting to access /api/auth/me now must be rejected with 403 Forbidden
        mockMvc.perform(get("/api/auth/me").cookie(sessionCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value(containsString("suspended")));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + sessionToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value(containsString("suspended")));

        // Update user status to DELETED
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);

        // Attempting to access /api/auth/me now must be rejected with 403 Forbidden
        mockMvc.perform(get("/api/auth/me").cookie(sessionCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value(containsString("deleted")));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + sessionToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value(containsString("deleted")));
    }

    @Test
    @DisplayName("Should authenticate seamlessly on other modules using session cookie and X-Session-Id header")
    void testCrossModuleSessionAuthentication() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("crossmodule@example.com")
                .password("Password123!")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.70")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie sessionCookie = result.getResponse().getCookie("session_token");
        assertNotNull(sessionCookie);

        String sessionId = sessionCookie.getValue();

        // 1. Call /api/notifications using session cookie
        mockMvc.perform(get("/api/notifications")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // 2. Call /api/notifications using Bearer session token
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // 3. Call /api/notifications using X-Session-Id header
        mockMvc.perform(get("/api/notifications")
                        .header("X-Session-Id", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Should block suspended user from accessing other modules (e.g. notifications)")
    void testSuspendedUserBlockedFromOtherModules() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("blockedmodule@example.com")
                .password("Password123!")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.80")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie sessionCookie = result.getResponse().getCookie("session_token");
        assertNotNull(sessionCookie);

        // Suspend user
        User user = userRepository.findByEmail("blockedmodule@example.com").orElseThrow();
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        // Attempting to access notifications must be rejected with 403 Forbidden
        mockMvc.perform(get("/api/notifications")
                        .cookie(sessionCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value(containsString("suspended")));
    }

    @Test
    @DisplayName("Should handle logout idempotently even if session token does not exist")
    void testLogoutIdempotence() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("session_token", "non-existent-token-xyz")))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("session_token", 0))
                .andExpect(jsonPath("$.data.message").value("Logged out successfully"));
    }

    @Test
    @DisplayName("Should create empty profile with null displayName when displayName is omitted in registration")
    void testRegisterWithOmittedDisplayName() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("no.display@example.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.91")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("session_token"))
                .andExpect(jsonPath("$.data.user.email").value("no.display@example.com"))
                .andExpect(jsonPath("$.data.profile.displayName").doesNotExist());
    }

    @Test
    @DisplayName("Should reject malformed email format variations on registration with 400 Bad Request")
    void testRegisterMalformedEmailVariations() throws Exception {
        String[] badEmails = {
                "plainaddress",
                "@missingusername.com",
                "missingdomain@.com",
                "two@@domain.com",
                "space in@domain.com"
        };

        for (int i = 0; i < badEmails.length; i++) {
            RegisterRequest req = RegisterRequest.builder()
                    .email(badEmails[i])
                    .password("Password123!")
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .header("X-Forwarded-For", "192.168.1.92")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when a non-existent or random session cookie is supplied")
    void testMeWithInvalidRandomCookieReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie("session_token", "random-bogus-token-value-000000000000")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("NOT_AUTHENTICATED"));
    }

    @Test
    @DisplayName("Should block deleted user from accessing notifications with 403 Forbidden")
    void testDeletedUserBlockedFromOtherModules() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("deletedmodule@example.com")
                .password("Password123!")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.93")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie sessionCookie = result.getResponse().getCookie("session_token");
        assertNotNull(sessionCookie);

        // Mark user status as DELETED
        User user = userRepository.findByEmail("deletedmodule@example.com").orElseThrow();
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);

        // Accessing notifications must be forbidden
        mockMvc.perform(get("/api/notifications")
                        .cookie(sessionCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value(containsString("deleted")));
    }

    @Test
    @DisplayName("Should allow conversation access with session cookie and block unauthenticated callers")
    void testCrossModuleConversationsWithSessionCookieAndUnauthenticatedBlocked() throws Exception {
        // 1. Unauthenticated call to /api/conversations should fail with 401
        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("NOT_AUTHENTICATED"));

        // 2. Register user to get valid session cookie
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("conv.session@example.com")
                .password("Password123!")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.94")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie sessionCookie = result.getResponse().getCookie("session_token");
        assertNotNull(sessionCookie);

        // 3. Authenticated call to /api/conversations with session cookie should succeed with 200 OK
        mockMvc.perform(get("/api/conversations")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // 4. Authenticated call to /api/conversations with Bearer session token should succeed with 200 OK
        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + sessionCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Should support concurrent sessions on multiple devices and isolate logout per session")
    void testConcurrentSessionsOnMultipleDevicesAndSingleSessionLogout() throws Exception {
        // Register user
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("multidevice@example.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.95")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Device 1: Login
        LoginRequest login1 = LoginRequest.builder()
                .email("multidevice@example.com")
                .password("Password123!")
                .build();

        MvcResult result1 = mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "192.168.1.95")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login1)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookieDevice1 = result1.getResponse().getCookie("session_token");
        assertNotNull(cookieDevice1);

        // Device 2: Login
        LoginRequest login2 = LoginRequest.builder()
                .email("multidevice@example.com")
                .password("Password123!")
                .build();

        MvcResult result2 = mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "192.168.1.96")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login2)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookieDevice2 = result2.getResponse().getCookie("session_token");
        assertNotNull(cookieDevice2);
        assertNotEquals(cookieDevice1.getValue(), cookieDevice2.getValue());

        // Both devices should be authenticated
        mockMvc.perform(get("/api/auth/me").cookie(cookieDevice1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("multidevice@example.com"));

        mockMvc.perform(get("/api/auth/me").cookie(cookieDevice2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("multidevice@example.com"));

        // Device 1 logs out
        mockMvc.perform(post("/api/auth/logout").cookie(cookieDevice1))
                .andExpect(status().isOk());

        // Device 1 is now logged out
        mockMvc.perform(get("/api/auth/me").cookie(cookieDevice1))
                .andExpect(status().isUnauthorized());

        // Device 2 MUST STILL BE LOGGED IN!
        mockMvc.perform(get("/api/auth/me").cookie(cookieDevice2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("multidevice@example.com"));
    }
}
