package com.truehunt.service;

import com.truehunt.config.JwtTokenProvider;
import com.truehunt.dto.request.LoginRequest;
import com.truehunt.dto.request.RegisterRequest;
import com.truehunt.dto.response.AuthResponse;
import com.truehunt.dto.response.ProfileResponse;
import com.truehunt.dto.response.UserResponse;
import com.truehunt.exception.ConflictException;
import com.truehunt.exception.ForbiddenException;
import com.truehunt.exception.ResourceNotFoundException;
import com.truehunt.exception.UnauthorizedException;
import com.truehunt.model.Profile;
import com.truehunt.model.User;
import com.truehunt.repository.ProfileRepository;
import com.truehunt.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ProfileService profileService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email is already registered.");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus("active");
        user = userRepository.save(user);

        // Create initial empty profile
        Profile profile = new Profile(user.getId());
        profile.setName(request.getName() != null && !request.getName().isBlank() ? request.getName().trim() : "Artist");
        profile.setEmail(normalizedEmail);
        profile = profileRepository.save(profile);

        String token = tokenProvider.generateToken(user.getId(), user.getEmail());

        UserResponse userResponse = new UserResponse(user.getId(), user.getEmail(), user.getStatus(), user.getCreatedAt());
        ProfileResponse profileResponse = profileService.toResponse(profile, true, false);

        return new AuthResponse(token, userResponse, profileResponse);
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new ForbiddenException("Account is currently " + user.getStatus() + ".");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getEmail());

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Profile p = new Profile(user.getId());
                    return profileRepository.save(p);
                });

        UserResponse userResponse = new UserResponse(user.getId(), user.getEmail(), user.getStatus(), user.getCreatedAt());
        ProfileResponse profileResponse = profileService.toResponse(profile, true, false);

        return new AuthResponse(token, userResponse, profileResponse);
    }

    public AuthResponse getMe(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found."));

        Profile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Profile p = new Profile(user.getId());
                    return profileRepository.save(p);
                });

        UserResponse userResponse = new UserResponse(user.getId(), user.getEmail(), user.getStatus(), user.getCreatedAt());
        ProfileResponse profileResponse = profileService.toResponse(profile, true, false);

        return new AuthResponse(null, userResponse, profileResponse);
    }
}
