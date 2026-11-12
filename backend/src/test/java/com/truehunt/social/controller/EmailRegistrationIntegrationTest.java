package com.truehunt.social.controller;

import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EmailRegistrationIntegrationTest {

    private static final String DUPLICATE_EMAIL_MSG =
            "This email is already registered. Please use another email address.";
    private static final AtomicInteger SEQUENCE = new AtomicInteger(20000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String nextIndianPhone(char leadingDigit) {
        long suffix = Math.abs((System.nanoTime() + SEQUENCE.incrementAndGet()) % 1_000_000_000L);
        return leadingDigit + String.format("%09d", suffix);
    }

    private String registrationJson(String email, String phoneNumber) {
        return """
                {
                  "fullName": "Email Tester",
                  "email": "%s",
                  "phoneNumber": "%s",
                  "password": "Password@123",
                  "confirmPassword": "Password@123",
                  "role": "TALENT",
                  "craft": "Acting"
                }
                """.formatted(email, phoneNumber);
    }

    @Test
    void firstRegistrationNormalizesEmailAndDuplicateCaseOrWhitespaceReturns409() throws Exception {
        long stamp = System.nanoTime();
        String normalizedEmail = "vamsi." + stamp + "@gmail.com";
        String mixedCaseEmail = "Vamsi." + stamp + "@Gmail.com";

        assertFalse(userAccountRepository.existsByEmail(normalizedEmail));
        assertFalse(userAccountRepository.existsByEmailIgnoreCase(mixedCaseEmail));

        // 1. Register new email (with mixed case and surrounding spaces -> normalized to lowercase)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("  " + mixedCaseEmail + "  ", nextIndianPhone('9'))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(normalizedEmail));

        assertTrue(userAccountRepository.existsByEmail(normalizedEmail));
        assertTrue(userAccountRepository.existsByEmailIgnoreCase(mixedCaseEmail));
        assertEquals(1, userAccountRepository.findAllByEmail(normalizedEmail).size());

        // 2. Same email exact lowercase -> 409 Conflict
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(normalizedEmail, nextIndianPhone('8'))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(DUPLICATE_EMAIL_MSG))
                .andExpect(jsonPath("$.errors.email").value(DUPLICATE_EMAIL_MSG));

        // 3. Same email uppercase -> 409 Conflict
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(normalizedEmail.toUpperCase(Locale.ROOT), nextIndianPhone('7'))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(DUPLICATE_EMAIL_MSG))
                .andExpect(jsonPath("$.errors.email").value(DUPLICATE_EMAIL_MSG));

        // 4. Same email with leading and trailing spaces -> 409 Conflict
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("   " + mixedCaseEmail + "   ", nextIndianPhone('6'))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(DUPLICATE_EMAIL_MSG))
                .andExpect(jsonPath("$.errors.email").value(DUPLICATE_EMAIL_MSG));

        assertEquals(1, userAccountRepository.findAllByEmail(normalizedEmail).size());

        // 5. Different valid email succeeds
        String otherEmail = "other." + stamp + "@gmail.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(otherEmail, nextIndianPhone('9'))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(otherEmail));
    }

    @Test
    void emptyAndInvalidEmailsFailValidationWith400() throws Exception {
        List<String> invalidEmails = List.of(
                "",
                "   ",
                "invalid-email",
                "vamsi@",
                "@gmail.com",
                "vamsi gmail@gmail.com"
        );

        for (String invalid : invalidEmails) {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registrationJson(invalid, nextIndianPhone('8'))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors.email").isNotEmpty());
        }
    }

    @Test
    void concurrentRegistrationsWithSameEmailAllowOnlyOneAccountAndReturn409ForLoser() throws Exception {
        long stamp = System.nanoTime();
        String sharedEmail = "concurrent." + stamp + "@gmail.com";
        String variantEmail = "  Concurrent." + stamp + "@GMAIL.com  ";

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        Callable<MvcResult> taskA = () -> {
            ready.countDown();
            assertTrue(go.await(5, TimeUnit.SECONDS));
            return mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registrationJson(sharedEmail, nextIndianPhone('9'))))
                    .andReturn();
        };
        Callable<MvcResult> taskB = () -> {
            ready.countDown();
            assertTrue(go.await(5, TimeUnit.SECONDS));
            return mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registrationJson(variantEmail, nextIndianPhone('8'))))
                    .andReturn();
        };

        Future<MvcResult> futureA = pool.submit(taskA);
        Future<MvcResult> futureB = pool.submit(taskB);
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        go.countDown();

        MvcResult resultA = futureA.get(10, TimeUnit.SECONDS);
        MvcResult resultB = futureB.get(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        List<Integer> statuses = List.of(resultA.getResponse().getStatus(), resultB.getResponse().getStatus());
        assertTrue(statuses.contains(200), "Expected one concurrent registration to succeed, got: " + statuses);
        assertTrue(statuses.contains(409), "Expected second concurrent registration to fail with 409, got: " + statuses);

        assertEquals(1, userAccountRepository.findAllByEmail(sharedEmail).size(),
                "Database UNIQUE constraint must ensure exactly one account per email");
    }

    @Test
    void databaseUniqueConstraintRejectsDirectDuplicateEmailInsert() {
        String uniqueEmail = "db.email.unique." + System.nanoTime() + "@gmail.com";

        UserAccountEntity first = new UserAccountEntity();
        first.setEmail(uniqueEmail);
        first.setPasswordHash(passwordEncoder.encode("Password@123"));
        first.setName("First Email Account");
        first.setPhoneNumber(nextIndianPhone('7'));
        first.setRole("TALENT");
        userAccountRepository.saveAndFlush(first);

        UserAccountEntity duplicate = new UserAccountEntity();
        duplicate.setEmail(uniqueEmail);
        duplicate.setPasswordHash(passwordEncoder.encode("Password@123"));
        duplicate.setName("Duplicate Email Account");
        duplicate.setPhoneNumber(nextIndianPhone('6'));
        duplicate.setRole("TALENT");

        assertThrows(DataIntegrityViolationException.class,
                () -> userAccountRepository.saveAndFlush(duplicate));
    }
}
