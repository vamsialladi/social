package com.social.service.impl;

import com.social.dto.request.CreateUserRequest;
import com.social.dto.response.UserProfileResponse;
import com.social.entity.User;
import com.social.exception.ConflictException;
import com.social.exception.ResourceNotFoundException;
import com.social.repository.UserRepository;
import com.social.service.UserService;
import com.social.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserProfileResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .id(IdGenerator.nextObjectId())
                .username(request.getUsername())
                .name(request.getName())
                .email(request.getEmail())
                .headline(request.getHeadline())
                .avatarUrl(request.getAvatarUrl())
                .bio(request.getBio())
                .role(request.getRole() != null ? request.getRole() : "ARTIST")
                .location(request.getLocation())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User saved = userRepository.save(user);
        log.info("Created user id: {}, username: {}", saved.getId(), saved.getUsername());
        return mapToProfile(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntity(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String userId) {
        return mapToProfile(getUserEntity(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToProfile)
                .collect(Collectors.toList());
    }

    public UserProfileResponse mapToProfile(User user) {
        if (user == null) {
            return null;
        }
        return UserProfileResponse.builder()
                .id(user.getId())
                .mongoId(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .headline(user.getHeadline())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .role(user.getRole())
                .location(user.getLocation())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
