package com.truehunt.social.service;

import com.truehunt.social.dto.AuthResponse;
import com.truehunt.social.dto.RegisterRequest;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import com.truehunt.social.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServicePublicUserIdTest {

    private UserAccountRepository userAccountRepository;
    private ProfileRepository profileRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userAccountRepository = mock(UserAccountRepository.class);
        profileRepository = mock(ProfileRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authService = new AuthService(
                profileRepository,
                userAccountRepository,
                passwordEncoder,
                new JwtTokenProvider()
        );

        when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userAccountRepository.findByPublicUserId(any())).thenReturn(Optional.empty());
        when(userAccountRepository.save(any(UserAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(profileRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(profileRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(profileRepository.save(any(ProfileEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(any())).thenReturn("encoded");
    }

    @Test
    void createsProducerIdWithProducerPrefixAndFiveDigits() {
        RegisterRequest request = registration("PRODUCER");

        AuthResponse response = authService.register(request);

        assertTrue(Pattern.matches("thPAM\\d{5}", response.getUserId()));
    }

    @Test
    void createsSocialIdWithSocialPrefixAndFiveDigits() {
        RegisterRequest request = registration("TALENT");

        AuthResponse response = authService.register(request);

        assertTrue(Pattern.matches("thscAM\\d{5}", response.getUserId()));
    }

    private RegisterRequest registration(String role) {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alex Morgan");
        request.setEmail("alex@example.com");
        request.setPassword("password");
        request.setRole(role);
        return request;
    }
}
