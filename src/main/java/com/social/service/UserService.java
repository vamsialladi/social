package com.social.service;

import com.social.dto.request.CreateUserRequest;
import com.social.dto.response.UserProfileResponse;
import com.social.entity.User;

import java.util.List;

public interface UserService {

    UserProfileResponse createUser(CreateUserRequest request);

    User getUserEntity(String userId);

    UserProfileResponse getUserProfile(String userId);

    List<UserProfileResponse> getAllUsers();
}
