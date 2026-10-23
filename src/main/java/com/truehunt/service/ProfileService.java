package com.truehunt.service;

import com.truehunt.dto.request.UpdateProfileRequest;
import com.truehunt.dto.response.ProfileResponse;
import com.truehunt.exception.ForbiddenException;
import com.truehunt.exception.ResourceNotFoundException;
import com.truehunt.model.Connection;
import com.truehunt.model.Profile;
import com.truehunt.repository.ConnectionRepository;
import com.truehunt.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ProfileService {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private MediaStorageService mediaStorageService;

    public ProfileResponse getProfile(String targetUserId, String currentUserId) {
        Profile profile = profileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + targetUserId));

        boolean isOwnProfile = currentUserId != null && currentUserId.equals(targetUserId);
        boolean isConnected = false;

        if (currentUserId != null && !isOwnProfile) {
            Optional<Connection> conn = connectionRepository.findConnectionBetween(currentUserId, targetUserId);
            isConnected = conn.isPresent() && "accepted".equals(conn.get().getStatus());
        }

        // If visibility is private and viewer is neither owner nor accepted connection
        if ("private".equalsIgnoreCase(profile.getVisibility()) && !isOwnProfile && !isConnected) {
            throw new ForbiddenException("This profile is private.");
        }

        return toResponse(profile, isOwnProfile, isConnected);
    }

    @Transactional
    public ProfileResponse updateMyProfile(String currentUserId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findByUserId(currentUserId)
                .orElseGet(() -> {
                    Profile p = new Profile(currentUserId);
                    return profileRepository.save(p);
                });

        if (request.getName() != null) {
            profile.setName(request.getName().trim());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone().trim());
        }
        if (request.getEmail() != null) {
            profile.setEmail(request.getEmail().trim());
        }
        if (request.getExperience() != null) {
            profile.setExperience(request.getExperience());
        }
        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress());
        }
        if (request.getLanguages() != null) {
            profile.setLanguages(request.getLanguages());
        }
        if (request.getOtherLanguage() != null) {
            profile.setOtherLanguage(request.getOtherLanguage());
        }
        if (request.getTalents() != null) {
            profile.setTalents(request.getTalents());
        }
        if (request.getWorkTypes() != null) {
            profile.setWorkTypes(request.getWorkTypes());
        }
        if (request.getProfileImageKey() != null) {
            profile.setProfileImageKey(request.getProfileImageKey());
        }
        if (request.getCoverImageKey() != null) {
            profile.setCoverImageKey(request.getCoverImageKey());
        }
        if (request.getDemoVideos() != null) {
            profile.setDemoVideos(request.getDemoVideos());
        }
        if (request.getVideoUrls() != null) {
            profile.setVideoUrls(request.getVideoUrls());
        }
        if (request.getVisibility() != null) {
            profile.setVisibility(request.getVisibility().toLowerCase());
        }

        profile = profileRepository.save(profile);
        return toResponse(profile, true, false);
    }

    public ProfileResponse toResponse(Profile profile, boolean isOwnProfile, boolean isConnected) {
        ProfileResponse res = new ProfileResponse();
        res.setId(profile.getId());
        res.setUserId(profile.getUserId());
        res.setName(profile.getName());
        res.setExperience(profile.getExperience());
        res.setLanguages(profile.getLanguages());
        res.setOtherLanguage(profile.getOtherLanguage());
        res.setTalents(profile.getTalents());
        res.setWorkTypes(profile.getWorkTypes());
        res.setProfileImageKey(profile.getProfileImageKey());
        res.setProfileImageUrl(mediaStorageService.buildMediaUrl(profile.getProfileImageKey()));
        res.setCoverImageKey(profile.getCoverImageKey());
        res.setCoverImageUrl(mediaStorageService.buildMediaUrl(profile.getCoverImageKey()));
        res.setDemoVideos(profile.getDemoVideos());
        res.setVideoUrls(profile.getVideoUrls());
        res.setVisibility(profile.getVisibility());
        res.setIsConnected(isConnected);
        res.setIsOwnProfile(isOwnProfile);
        res.setCreatedAt(profile.getCreatedAt());
        res.setUpdatedAt(profile.getUpdatedAt());

        // Contact fields private by default: returned ONLY if own profile or connected
        if (isOwnProfile || isConnected) {
            res.setPhone(profile.getPhone());
            res.setEmail(profile.getEmail());
            res.setAddress(profile.getAddress());
        } else {
            res.setPhone(null);
            res.setEmail(null);
            res.setAddress(null);
        }

        return res;
    }
}
