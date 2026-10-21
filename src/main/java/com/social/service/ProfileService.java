package com.social.service;

import com.social.dto.request.ProfileUpdateRequest;
import com.social.dto.response.ProfileResponse;

public interface ProfileService {
    ProfileResponse getProfile(String targetUserId, String viewerUserId);
    ProfileResponse updateMyProfile(String currentUserId, ProfileUpdateRequest request);
}
