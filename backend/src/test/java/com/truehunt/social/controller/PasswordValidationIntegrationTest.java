package com.truehunt.social.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truehunt.social.model.PasswordResetTokenEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.PasswordResetTokenRepository;
import com.truehunt.social.repository.UserAccountRepository;
import com.truehunt.social.security.LoginRateLimiter;
import com.truehunt.social.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginRateLimiter loginRateLimiter;

    @MockitoSpyBean
    private AuthService authService;

    @BeforeEach
    void resetRateLimits() {
        loginRateLimiter.resetAll();
    }

    @Test
    void registrationRejectsAllInvalidPasswordScenariosWithStructuredErrors() throws Exception {
        List<String> invalidPasswords = List.of(
                "",                           // 1. Empty password
                "   ",                        // 1b. Blank password
                "Ab1@xyz",                    // 2. Shorter than 8 characters
                "Ab1@cdefghij123456789",      // 3. Longer than 20 characters (21 chars)
                "password@1",                 // 4. Missing uppercase
                "PASSWORD@1",                 // 5. Missing lowercase
                "Password@#",                 // 6. Missing number
                "Password12",                 // 7. Missing special character
                "Pass word@1"                 // 8. Contains space
        );

        for (String invalid : invalidPasswords) {
            String email = "pw.invalid." + System.nanoTime() + "@truehunt.in";
            String payload = objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
                put("fullName", "Validation Tester");
                put("email", email);
                put("password", invalid);
                put("confirmPassword", invalid);
                put("role", "TALENT");
                put("craft", "Director");
            }});

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Password validation failed"))
                    .andExpect(jsonPath("$.errors.password").isNotEmpty());

            assertTrue(userAccountRepository.findByEmail(email).isEmpty(),
                    "Invalid password should not create user account for: " + invalid);
        }
    }

    @Test
    void registrationRejectsConfirmPasswordMismatchEvenWhenBypassingFrontend() throws Exception {
        String email = "pw.mismatch." + System.nanoTime() + "@truehunt.in";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Mismatch User",
                                  "email": "%s",
                                  "password": "Cinema@2026",
                                  "confirmPassword": "Cinema@2027",
                                  "role": "TALENT",
                                  "craft": "Editor"
                                }
                                """.formatted(email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Password validation failed"))
                .andExpect(jsonPath("$.errors.confirmPassword").value("Password and confirm password do not match"));
    }

    @Test
    void registrationHashesPasswordWithBcryptNeverExposesHashAndSupportsLoginRateLimiting() throws Exception {
        String email = "pw.valid." + System.nanoTime() + "@truehunt.in";
        String strongPassword = "Cinema@2026";

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Priya Nair",
                                  "email": "%s",
                                  "password": "%s",
                                  "confirmPassword": "%s",
                                  "role": "TALENT",
                                  "craft": "Director"
                                }
                                """.formatted(email, strongPassword, strongPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        String regBody = regResult.getResponse().getContentAsString();
        assertFalse(regBody.contains(strongPassword), "Response must never echo plaintext password");
        assertFalse(regBody.contains("passwordHash"), "Response must never expose passwordHash");

        UserAccountEntity stored = userAccountRepository.findByEmail(email).orElseThrow();
        assertNotEquals(strongPassword, stored.getPasswordHash());
        assertTrue(stored.getPasswordHash().startsWith("$2"), "Password must be BCrypt encoded");
        assertTrue(passwordEncoder.matches(strongPassword, stored.getPasswordHash()));

        // Login with correct password succeeds
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, strongPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        String loginBody = loginResult.getResponse().getContentAsString();
        assertFalse(loginBody.contains(strongPassword));
        assertFalse(loginBody.contains("passwordHash"));

        // Login with incorrect password fails and triggers rate limit after 5 failed attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "%s",
                                      "password": "Wrong@Password9"
                                    }
                                    """.formatted(email)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, strongPassword)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void changePasswordVerifiesCurrentPasswordValidatesNewPasswordAndConfirmMatch() throws Exception {
        String email = "pw.change." + System.nanoTime() + "@truehunt.in";
        String initialPassword = "Cinema@2026";
        String nextPassword = "Studio#2027";

        MvcResult regRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Arjun Mehta",
                                  "email": "%s",
                                  "password": "%s",
                                  "confirmPassword": "%s",
                                  "role": "TALENT",
                                  "craft": "Acting"
                                }
                                """.formatted(email, initialPassword, initialPassword)))
                .andExpect(status().isOk())
                .andReturn();
        String userId = objectMapper.readTree(regRes.getResponse().getContentAsString())
                .path("data").path("userId").asText();

        // Reject invalid new password
        mockMvc.perform(post("/api/settings/change-password")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "%s",
                                  "newPassword": "weak",
                                  "confirmPassword": "weak"
                                }
                                """.formatted(initialPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Password validation failed"))
                .andExpect(jsonPath("$.errors.newPassword").isNotEmpty());

        // Reject mismatched confirm password
        mockMvc.perform(post("/api/settings/change-password")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "%s",
                                  "newPassword": "%s",
                                  "confirmPassword": "Different#1"
                                }
                                """.formatted(initialPassword, nextPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.confirmPassword").value("Password and confirm password do not match"));

        // Reject wrong current password
        mockMvc.perform(post("/api/settings/change-password")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Wrong@Password1",
                                  "newPassword": "%s",
                                  "confirmPassword": "%s"
                                }
                                """.formatted(nextPassword, nextPassword)))
                .andExpect(status().isBadRequest());

        // Succeed with valid current + new + confirm password
        mockMvc.perform(post("/api/settings/change-password")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "%s",
                                  "newPassword": "%s",
                                  "confirmPassword": "%s"
                                }
                                """.formatted(initialPassword, nextPassword, nextPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        UserAccountEntity updated = userAccountRepository.findByEmail(email).orElseThrow();
        assertTrue(passwordEncoder.matches(nextPassword, updated.getPasswordHash()));
    }

    @Test
    void forgotAndResetPasswordSupportsValidTokenRejectsExpiredAndReusedTokens() throws Exception {
        String email = "pw.reset." + System.nanoTime() + "@truehunt.in";
        String originalPassword = "Cinema@2026";
        String resetPassword = "Director$2028";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Meera Iyer",
                                  "email": "%s",
                                  "password": "%s",
                                  "confirmPassword": "%s",
                                  "role": "TALENT",
                                  "craft": "Editing"
                                }
                                """.formatted(email, originalPassword, originalPassword)))
                .andExpect(status().isOk());

        // 1. Request reset for existing account -> does NOT expose token in HTTP response
        MvcResult forgotRes = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(authService, atLeastOnce()).deliverPasswordResetToken(eq(email), tokenCaptor.capture());
        String firstToken = tokenCaptor.getValue();
        assertNotNull(firstToken);
        assertFalse(forgotRes.getResponse().getContentAsString().contains(firstToken),
                "HTTP response must never expose raw reset token");

        // Database must store only the SHA-256 hash, never the raw token
        List<PasswordResetTokenEntity> storedTokens = passwordResetTokenRepository.findByEmailIgnoreCaseAndUsedFalse(email);
        assertEquals(1, storedTokens.size());
        assertNotEquals(firstToken, storedTokens.get(0).getTokenHash());
        assertEquals(64, storedTokens.get(0).getTokenHash().length());

        // 2. Weak password rejected even with valid token
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "newPassword": "weakpassword",
                                  "confirmPassword": "weakpassword"
                                }
                                """.formatted(firstToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password validation failed"))
                .andExpect(jsonPath("$.errors.newPassword").isNotEmpty());

        // 3. Valid token resets password and updates BCrypt hash
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "newPassword": "%s",
                                  "confirmPassword": "%s"
                                }
                                """.formatted(firstToken, resetPassword, resetPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. Reusing the same token fails
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "newPassword": "Another@Password1",
                                  "confirmPassword": "Another@Password1"
                                }
                                """.formatted(firstToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // 5. Login with new password succeeds, old password fails
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, resetPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, originalPassword)))
                .andExpect(status().isUnauthorized());

        // 6. Expired token fails
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isOk());

        ArgumentCaptor<String> secondTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(authService, atLeastOnce()).deliverPasswordResetToken(eq(email), secondTokenCaptor.capture());
        String secondToken = secondTokenCaptor.getValue();

        List<PasswordResetTokenEntity> activeSecond = passwordResetTokenRepository.findByEmailIgnoreCaseAndUsedFalse(email);
        assertEquals(1, activeSecond.size());
        PasswordResetTokenEntity expiredEntity = activeSecond.get(0);
        expiredEntity.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        passwordResetTokenRepository.save(expiredEntity);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "newPassword": "Fresh@Password1",
                                  "confirmPassword": "Fresh@Password1"
                                }
                                """.formatted(secondToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}

