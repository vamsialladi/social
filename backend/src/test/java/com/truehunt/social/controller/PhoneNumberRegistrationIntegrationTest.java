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

import java.util.ArrayList;
import java.util.List;
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
class PhoneNumberRegistrationIntegrationTest {

    private static final String DUPLICATE_PHONE_MSG =
            "This phone number is already registered. Please use another phone number.";
    private static final AtomicInteger SEQUENCE = new AtomicInteger(10000);

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
                  "fullName": "Phone Tester",
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
    void firstRegistrationSucceedsAndDuplicatePhoneRejectsWith409AcrossFormats() throws Exception {
        String phone = nextIndianPhone('9');
        String firstEmail = "phone.first." + System.nanoTime() + "@truehunt.in";

        assertFalse(userAccountRepository.existsByPhoneNumber(phone));

        // 1. Sending/verifying OTP does NOT treat number as registered
        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phone + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phone + "\",\"otp\":\"000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false));
        assertFalse(userAccountRepository.existsByPhoneNumber(phone),
                "Uncompleted / failed OTP must not register phone number");

        // 2. First registration with valid Indian phone succeeds
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(firstEmail, phone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phone").value(phone));

        assertTrue(userAccountRepository.existsByPhoneNumber(phone));
        assertEquals(1, userAccountRepository.findAllByPhoneNumber(phone).size());

        // 3. Second registration with exact same number OR different formatting fails with 409 Conflict
        List<String> duplicateFormats = List.of(
                phone,
                " " + phone + " ",
                "+91 " + phone,
                "+91-" + phone,
                "91" + phone,
                "0" + phone,
                phone.substring(0, 5) + " " + phone.substring(5)
        );
        for (String duplicateFormat : duplicateFormats) {
            String secondEmail = "phone.dup." + System.nanoTime() + "@truehunt.in";
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registrationJson(secondEmail, duplicateFormat)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(DUPLICATE_PHONE_MSG))
                    .andExpect(jsonPath("$.errors.phoneNumber").value(DUPLICATE_PHONE_MSG));

            assertTrue(userAccountRepository.findByEmail(secondEmail).isEmpty(),
                    "Duplicate phone registration must not save account for: " + duplicateFormat);
        }

        assertEquals(1, userAccountRepository.findAllByPhoneNumber(phone).size());

        // 4. Different valid Indian mobile number succeeds
        String differentPhone = nextIndianPhone('6');
        String thirdEmail = "phone.other." + System.nanoTime() + "@truehunt.in";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(thirdEmail, differentPhone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phone").value(differentPhone));

        assertTrue(userAccountRepository.existsByPhoneNumber(differentPhone));
    }

    @Test
    void rejectsInvalidIndianMobileNumbersWith400BadRequest() throws Exception {
        List<String> invalidPhones = List.of(
                "",
                "   ",
                "98765",
                "98765432101",
                "5876543210",
                "1234567890",
                "98765abc10",
                "98765@3210",
                "98765#3210",
                "+1 9876543210"
        );

        for (String invalid : invalidPhones) {
            String email = "phone.invalid." + System.nanoTime() + "@truehunt.in";
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registrationJson(email, invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors.phoneNumber").isNotEmpty());

            assertTrue(userAccountRepository.findByEmail(email).isEmpty());
        }
    }

    @Test
    void concurrentRegistrationsWithSamePhoneAllowOnlyOneAccountAndReturn409ForLoser() throws Exception {
        String sharedPhone = nextIndianPhone('8');
        String emailA = "phone.concurrent.a." + System.nanoTime() + "@truehunt.in";
        String emailB = "phone.concurrent.b." + System.nanoTime() + "@truehunt.in";

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        Callable<MvcResult> taskA = () -> {
            ready.countDown();
            assertTrue(go.await(5, TimeUnit.SECONDS));
            return mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registrationJson(emailA, sharedPhone)))
                    .andReturn();
        };
        Callable<MvcResult> taskB = () -> {
            ready.countDown();
            assertTrue(go.await(5, TimeUnit.SECONDS));
            return mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registrationJson(emailB, sharedPhone)))
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

        List<UserAccountEntity> saved = new ArrayList<>(userAccountRepository.findAllByPhoneNumber(sharedPhone));
        assertEquals(1, saved.size(), "Database UNIQUE constraint must ensure exactly one account per phone");
    }

    @Test
    void databaseUniqueConstraintRejectsDirectDuplicateInsert() {
        String uniquePhone = nextIndianPhone('7');
        UserAccountEntity first = new UserAccountEntity();
        first.setEmail("db.unique.a." + System.nanoTime() + "@truehunt.in");
        first.setPasswordHash(passwordEncoder.encode("Password@123"));
        first.setName("First Account");
        first.setPhoneNumber(uniquePhone);
        first.setRole("TALENT");
        userAccountRepository.saveAndFlush(first);

        UserAccountEntity duplicate = new UserAccountEntity();
        duplicate.setEmail("db.unique.b." + System.nanoTime() + "@truehunt.in");
        duplicate.setPasswordHash(passwordEncoder.encode("Password@123"));
        duplicate.setName("Duplicate Account");
        duplicate.setPhoneNumber(uniquePhone);
        duplicate.setRole("TALENT");

        assertThrows(DataIntegrityViolationException.class,
                () -> userAccountRepository.saveAndFlush(duplicate));
    }
}

