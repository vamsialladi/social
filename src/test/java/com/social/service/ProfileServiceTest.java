package com.social.service;

import com.social.dto.request.ProfileUpdateRequest;
import com.social.dto.response.MediaFileReferenceDto;
import com.social.dto.response.ProfileResponse;
import com.social.entity.Profile;
import com.social.entity.ProfileVisibility;
import com.social.exception.BadRequestException;
import com.social.exception.ForbiddenException;
import com.social.exception.ResourceNotFoundException;
import com.social.repository.ProfileRepository;
import com.social.service.impl.ProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(profileService, "mediaBaseUrl", "/api/media/files/");
        ReflectionTestUtils.setField(profileService, "maxDemoVideoSizeBytes", 5242880L);
    }

    @Test
    void getProfile_authorizedViewer_returnsPhoneAndEmail() {
        Profile profile = Profile.builder()
                .id("prof-1")
                .userId("user-100")
                .name("Artist Name")
                .phone("+1234567890")
                .email("artist@example.com")
                .visibility(ProfileVisibility.PUBLIC)
                .build();

        when(profileRepository.findByUserId("user-100")).thenReturn(Optional.of(profile));

        ProfileResponse response = profileService.getProfile("user-100", "user-100");

        assertNotNull(response);
        assertEquals("user-100", response.getUserId());
        assertEquals("+1234567890", response.getPhone());
        assertEquals("artist@example.com", response.getEmail());
    }

    @Test
    void getProfile_unauthorizedViewer_hidesPhoneAndEmail() {
        Profile profile = Profile.builder()
                .id("prof-1")
                .userId("user-100")
                .name("Artist Name")
                .phone("+1234567890")
                .email("artist@example.com")
                .visibility(ProfileVisibility.PUBLIC)
                .build();

        when(profileRepository.findByUserId("user-100")).thenReturn(Optional.of(profile));

        ProfileResponse response = profileService.getProfile("user-100", "viewer-200");

        assertNotNull(response);
        assertEquals("user-100", response.getUserId());
        assertNull(response.getPhone(), "Phone should be hidden from non-owners");
        assertNull(response.getEmail(), "Email should be hidden from non-owners");
    }

    @Test
    void getProfile_privateProfile_unauthorizedViewer_throwsForbiddenException() {
        Profile profile = Profile.builder()
                .id("prof-1")
                .userId("user-100")
                .name("Artist Name")
                .visibility(ProfileVisibility.PRIVATE)
                .build();

        when(profileRepository.findByUserId("user-100")).thenReturn(Optional.of(profile));

        assertThrows(ForbiddenException.class, () -> 
                profileService.getProfile("user-100", "viewer-200"));
    }

    @Test
    void getProfile_notFound_throwsResourceNotFoundException() {
        when(profileRepository.findByUserId("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                profileService.getProfile("unknown", "user-100"));
    }

    @Test
    void updateMyProfile_validData_succeeds() {
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .name("Updated Artist")
                .phone("+9876543210")
                .email("updated@example.com")
                .experience("5 years of theatre")
                .languages(List.of("English", "Spanish"))
                .talents(List.of("Acting", "Voiceover"))
                .videoUrls(List.of("https://youtube.com/watch?v=12345"))
                .visibility(ProfileVisibility.PUBLIC)
                .build();

        when(profileRepository.findByUserId("user-100")).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileResponse response = profileService.updateMyProfile("user-100", request);

        assertNotNull(response);
        assertEquals("Updated Artist", response.getName());
        assertEquals("+9876543210", response.getPhone());
        assertEquals(2, response.getLanguages().size());
        assertEquals(1, response.getVideoUrls().size());
    }

    @Test
    void updateMyProfile_demoVideoExceeds5MB_throwsBadRequestException() {
        MediaFileReferenceDto largeVideo = MediaFileReferenceDto.builder()
                .fileName("reel.mp4")
                .objectKey("key-1")
                .sizeBytes(6 * 1024 * 1024L) // 6 MB > 5 MB
                .build();

        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .name("Artist")
                .demoVideos(List.of(largeVideo))
                .build();

        assertThrows(BadRequestException.class, () -> 
                profileService.updateMyProfile("user-100", request));
    }

    @Test
    void updateMyProfile_invalidVideoUrl_throwsBadRequestException() {
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .name("Artist")
                .videoUrls(List.of("invalid-not-a-url"))
                .build();

        assertThrows(BadRequestException.class, () -> 
                profileService.updateMyProfile("user-100", request));
    }
}
