package com.truehunt.social.service;

import com.truehunt.social.dto.ProfileDto;
import com.truehunt.social.dto.ProfileUpdateRequest;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.model.UserProfileRelationshipEntity;
import com.truehunt.social.repository.PostRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.ProfileViewRepository;
import com.truehunt.social.repository.UserAccountRepository;
import com.truehunt.social.repository.UserProfileRelationshipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileServiceUpdateByPublicIdTest {

    private static final String PUBLIC_ID = "thscAL12345";

    private ProfileEntity profile;
    private UserAccountEntity account;
    private ProfileRepository profileRepository;
    private UserAccountRepository userAccountRepository;
    private UserProfileRelationshipRepository relationshipRepository;
    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        profileRepository = mock(ProfileRepository.class);
        userAccountRepository = mock(UserAccountRepository.class);
        relationshipRepository = mock(UserProfileRelationshipRepository.class);
        profile = new ProfileEntity();
        profile.setUserId(PUBLIC_ID);
        profile.setName("Alex Morgan");
        profile.setEmail("alex@example.com");
        profile.setPhone("+15551234567");
        profile.setProfessionalPhone("+15559876543");

        account = new UserAccountEntity();
        account.setId(42L);
        account.setPublicUserId(PUBLIC_ID);
        account.setEmail("alex@example.com");
        account.setName("Alex Morgan");

        when(profileRepository.findByUserId(anyString())).thenReturn(Optional.of(profile));
        when(profileRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(profileRepository.save(any(ProfileEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userAccountRepository.findByPublicUserId(anyString())).thenReturn(Optional.of(account));
        when(userAccountRepository.save(any(UserAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(relationshipRepository.findAll()).thenReturn(List.of());

        profileService = new ProfileService(
                profileRepository,
                userAccountRepository,
                mock(PostRepository.class),
                mock(ProfileViewRepository.class),
                relationshipRepository,
                mock(ObjectProvider.class)
        );
    }

    @Test
    void savesProfileImageWhenRequestIdentifiesAccountByPublicId() {
        String imageDataUrl = "data:image/jpeg;base64,cropped-image";
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setUserId(PUBLIC_ID);
        request.setProfileImageUrl(imageDataUrl);
        request.setEmail("alex@example.com");

        profileService.updateMyProfile(request, PUBLIC_ID);

        assertEquals(imageDataUrl, profile.getProfileImageUrl());
        assertEquals(PUBLIC_ID, account.getPublicUserId());
    }

    @Test
    void resolvesCurrentProfileFromAccountWhenOnlyPublicIdMatches() {
        when(profileRepository.findByUserId(PUBLIC_ID)).thenReturn(Optional.empty());

        ProfileEntity resolved = profileService.findCurrentProfile(PUBLIC_ID);

        assertEquals(PUBLIC_ID, resolved.getUserId());
        assertEquals("alex@example.com", resolved.getEmail());
    }

    @Test
    void doesNotPersistASocialProfileForAdminAccount() {
        UserAccountEntity admin = new UserAccountEntity();
        admin.setId(7L);
        admin.setPublicUserId("THADMIN1");
        admin.setEmail("admin@example.com");
        admin.setName("Platform Admin");
        admin.setRole("ADMIN");
        when(userAccountRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(profileRepository.findByUserId("admin@example.com")).thenReturn(Optional.empty());

        ProfileEntity resolved = profileService.findCurrentProfile("admin@example.com");

        assertEquals("THADMIN1", resolved.getUserId());
        assertEquals("admin@example.com", resolved.getEmail());
        verify(profileRepository, never()).save(any(ProfileEntity.class));
    }

    @Test
    void hidesContactDetailsFromProfilesWithoutAnAcceptedConnection() {
        when(userAccountRepository.findByPublicUserId("viewer@example.com")).thenReturn(Optional.empty());
        when(profileRepository.findByUserId("viewer@example.com")).thenReturn(Optional.empty());

        ProfileDto result = profileService.getProfileById(PUBLIC_ID, "viewer@example.com");

        assertNull(result.getPhone());
        assertNull(result.getEmail());
        assertNull(result.getProfessionalPhone());
    }

    @Test
    void hidesAllContactDetailsFromConnectedProfilesUntilContactAccessIsApproved() {
        when(userAccountRepository.findByPublicUserId("viewer@example.com")).thenReturn(Optional.empty());
        when(profileRepository.findByUserId("viewer@example.com")).thenReturn(Optional.empty());
        UserProfileRelationshipEntity connection = new UserProfileRelationshipEntity();
        connection.setOwnerUserId("viewer@example.com");
        connection.setTargetProfileId(PUBLIC_ID);
        connection.setConnected(true);
        when(relationshipRepository.findAll()).thenReturn(List.of(connection));

        ProfileDto result = profileService.getProfileById(PUBLIC_ID, "viewer@example.com");

        assertNull(result.getEmail());
        assertNull(result.getProfessionalPhone());
        assertNull(result.getPhone());
        assertTrue(result.getIsConnected());
    }

    @Test
    void anonymousViewerCannotInheritFallbackOwnerIdentity() {
        profile.setUserId("1000001");
        profile.setEmail("pro@truehunt.in");
        account.setPublicUserId("1000001");
        account.setEmail("pro@truehunt.in");

        ProfileDto result = profileService.getProfileById("1000001", null);

        assertNull(result.getPhone());
        assertNull(result.getEmail());
        assertNull(result.getProfessionalPhone());
        assertFalse(result.getIsOwner());
    }
}
