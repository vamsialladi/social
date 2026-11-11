package com.truehunt.social.service;

import com.truehunt.social.dto.ProfileUpdateRequest;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.PostRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.ProfileViewRepository;
import com.truehunt.social.repository.UserAccountRepository;
import com.truehunt.social.repository.UserProfileRelationshipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileServiceUpdateByPublicIdTest {

    private static final String PUBLIC_ID = "thscAL12345";

    private ProfileEntity profile;
    private UserAccountEntity account;
    private ProfileRepository profileRepository;
    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileRepository = mock(ProfileRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        UserProfileRelationshipRepository relationshipRepository = mock(UserProfileRelationshipRepository.class);
        profile = new ProfileEntity();
        profile.setUserId(PUBLIC_ID);
        profile.setName("Alex Morgan");
        profile.setEmail("alex@example.com");

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
}
