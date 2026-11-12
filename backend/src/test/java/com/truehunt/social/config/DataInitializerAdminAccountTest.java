package com.truehunt.social.config;

import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataInitializerAdminAccountTest {

    @Test
    void provisionsConfiguredAdminCredentials() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        when(repository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(repository.existsByPublicUserId(anyString())).thenReturn(false);
        when(repository.save(any(UserAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new DataInitializer().provisionAdminAccount(
                repository,
                passwordEncoder,
                "ADMIN@EXAMPLE.COM",
                "configured-password"
        );

        org.mockito.ArgumentCaptor<UserAccountEntity> captor =
                org.mockito.ArgumentCaptor.forClass(UserAccountEntity.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        UserAccountEntity admin = captor.getValue();
        assertEquals("admin@example.com", admin.getEmail());
        assertEquals("ADMIN", admin.getRole());
        assertTrue(passwordEncoder.matches("configured-password", admin.getPasswordHash()));
    }
}
