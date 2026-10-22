package com.social.service.impl;

import com.social.dto.request.ProfileUpdateRequest;
import com.social.dto.response.MediaFileReferenceDto;
import com.social.dto.response.ProfileResponse;
import com.social.entity.MediaFileReference;
import com.social.entity.Profile;
import com.social.entity.ProfileVisibility;
import com.social.exception.BadRequestException;
import com.social.exception.ForbiddenException;
import com.social.exception.ResourceNotFoundException;
import com.social.repository.ProfileRepository;
import com.social.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProfileServiceImpl implements ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private final ProfileRepository profileRepository;

    @Value("${app.media.base-url:/api/media/files/}")
    private String mediaBaseUrl;

    @Value("${app.media.max-demo-video-size-bytes:5242880}") // 5 MB
    private long maxDemoVideoSizeBytes;

    public ProfileServiceImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String targetUserId, String viewerUserId) {
        if (!StringUtils.hasText(targetUserId)) {
            throw new BadRequestException("Target userId is required");
        }

        Profile profile = profileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for userId: " + targetUserId));

        boolean isAuthorized = viewerUserId != null && viewerUserId.trim().equalsIgnoreCase(targetUserId.trim());

        // Visibility check: if profile is private and viewer is not authorized
        if (profile.getVisibility() == ProfileVisibility.PRIVATE && !isAuthorized) {
            throw new ForbiddenException("This profile is private.");
        }

        return ProfileResponse.fromEntity(profile, isAuthorized, mediaBaseUrl);
    }

    @Override
    @Transactional
    public ProfileResponse updateMyProfile(String currentUserId, ProfileUpdateRequest request) {
        if (!StringUtils.hasText(currentUserId)) {
            throw new BadRequestException("User ID is required to update profile");
        }

        // Validate demo video sizes (max 5 MB each)
        if (request.getDemoVideos() != null) {
            for (MediaFileReferenceDto video : request.getDemoVideos()) {
                if (video.getSizeBytes() != null && video.getSizeBytes() > maxDemoVideoSizeBytes) {
                    throw new BadRequestException("Demo video '" + video.getFileName() + 
                            "' exceeds the maximum allowed size of 5 MB (" + video.getSizeBytes() + " bytes)");
                }
            }
        }

        // Validate video URLs format
        if (request.getVideoUrls() != null) {
            for (String urlStr : request.getVideoUrls()) {
                if (StringUtils.hasText(urlStr)) {
                    validateUrl(urlStr.trim());
                }
            }
        }

        // Fetch or create profile for current user
        Profile profile = profileRepository.findByUserId(currentUserId)
                .orElseGet(() -> Profile.builder()
                        .userId(currentUserId)
                        .name(StringUtils.hasText(request.getName()) ? request.getName().trim() : "Artist")
                        .visibility(ProfileVisibility.PUBLIC)
                        .build());

        // Update fields
        if (StringUtils.hasText(request.getName())) {
            profile.setName(request.getName().trim());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone().trim());
        }
        if (request.getEmail() != null) {
            profile.setEmail(request.getEmail().trim());
        }
        if (request.getExperience() != null) {
            profile.setExperience(request.getExperience().trim());
        }
        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress().trim());
        }
        if (request.getLanguages() != null) {
            profile.setLanguages(new ArrayList<>(request.getLanguages()));
        }
        if (request.getOtherLanguage() != null) {
            profile.setOtherLanguage(request.getOtherLanguage().trim());
        }
        if (request.getTalents() != null) {
            profile.setTalents(new ArrayList<>(request.getTalents()));
        }
        if (request.getWorkTypes() != null) {
            profile.setWorkTypes(new ArrayList<>(request.getWorkTypes()));
        }
        if (request.getProfileImageKey() != null) {
            profile.setProfileImageKey(request.getProfileImageKey().trim());
        }
        if (request.getCoverImageKey() != null) {
            profile.setCoverImageKey(request.getCoverImageKey().trim());
        }
        if (request.getDemoVideos() != null) {
            List<MediaFileReference> references = request.getDemoVideos().stream()
                    .map(MediaFileReferenceDto::toEntity)
                    .toList();
            profile.setDemoVideos(new ArrayList<>(references));
        }
        if (request.getVideoUrls() != null) {
            profile.setVideoUrls(new ArrayList<>(request.getVideoUrls()));
        }
        if (request.getVisibility() != null) {
            profile.setVisibility(request.getVisibility());
        }

        Profile saved = profileRepository.save(profile);
        log.info("Profile updated successfully for user: {}", currentUserId);

        return ProfileResponse.fromEntity(saved, true, mediaBaseUrl);
    }

    private void validateUrl(String urlString) {
        try {
            java.net.URI uri = new java.net.URI(urlString);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new BadRequestException("Invalid URL protocol in '" + urlString + "'. Only HTTP/HTTPS is allowed.");
            }
            if (uri.getHost() == null) {
                throw new BadRequestException("Invalid URL host in '" + urlString + "'.");
            }
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new BadRequestException("Invalid video URL format: " + urlString);
        }
    }
}
