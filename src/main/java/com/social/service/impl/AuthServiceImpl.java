package com.social.service.impl;

import com.social.dto.request.LoginRequest;
import com.social.dto.request.RegisterRequest;
import com.social.dto.response.AuthResponse;
import com.social.dto.response.ProfileResponse;
import com.social.dto.response.UserResponse;
import com.social.entity.Profile;
import com.social.entity.User;
import com.social.entity.UserSession;
import com.social.entity.UserStatus;
import com.social.exception.ConflictException;
import com.social.exception.ForbiddenException;
import com.social.exception.ResourceNotFoundException;
import com.social.exception.UnauthorizedException;
import com.social.repository.ProfileRepository;
import com.social.repository.UserRepository;
import com.social.repository.UserSessionRepository;
import com.social.service.AuthService;
import com.social.service.RateLimiterService;
import com.social.util.IdGenerator;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiterService rateLimiterService;

    private final int loginMaxRequests;
    private final long loginWindowSeconds;
    private final int registerMaxRequests;
    private final long registerWindowSeconds;
    private final String cookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final long sessionDurationSeconds;

    public AuthServiceImpl(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            UserSessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            RateLimiterService rateLimiterService,
            @Value("${app.auth.rate-limit.login.max-requests:5}") int loginMaxRequests,
            @Value("${app.auth.rate-limit.login.window-seconds:60}") long loginWindowSeconds,
            @Value("${app.auth.rate-limit.register.max-requests:5}") int registerMaxRequests,
            @Value("${app.auth.rate-limit.register.window-seconds:60}") long registerWindowSeconds,
            @Value("${app.auth.cookie.name:session_token}") String cookieName,
            @Value("${app.auth.cookie.secure:false}") boolean cookieSecure,
            @Value("${app.auth.cookie.same-site:Lax}") String cookieSameSite,
            @Value("${app.auth.cookie.max-age-seconds:604800}") long sessionDurationSeconds) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiterService = rateLimiterService;
        this.loginMaxRequests = loginMaxRequests;
        this.loginWindowSeconds = loginWindowSeconds;
        this.registerMaxRequests = registerMaxRequests;
        this.registerWindowSeconds = registerWindowSeconds;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.sessionDurationSeconds = sessionDurationSeconds;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, String clientIp, HttpServletResponse response) {
        // 1. Rate-limit registration
        rateLimiterService.checkRateLimit("auth:register:" + clientIp, registerMaxRequests, registerWindowSeconds, "registration attempts");

        // 2. Normalize email
        String email = normalizeEmail(request.getEmail());

        // 3. Ensure email uniqueness
        if (userRepository.existsByEmail(email)) {
            log.warn("Registration rejected: email {} is already in use", email);
            throw new ConflictException("Email already in use");
        }

        // 4. Create user record with BCrypt password hash
        String userId = IdGenerator.nextId();
        String passwordHash = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .id(userId)
                .email(email)
                .passwordHash(passwordHash)
                .status(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);
        log.info("Registered new user with id: {} and email: {}", userId, email);

        // 5. Create empty profile for user (one profile per user)
        String profileId = IdGenerator.nextId();
        Profile profile = Profile.builder()
                .id(profileId)
                .userId(userId)
                .displayName(StringUtils.hasText(request.getDisplayName()) ? request.getDisplayName().trim() : null)
                .build();
        profile = profileRepository.save(profile);
        log.info("Created empty profile with id: {} for user: {}", profileId, userId);

        // 6. Start session and issue HttpOnly cookie
        String sessionId = createSession(userId);
        attachSessionCookie(response, sessionId, sessionDurationSeconds);

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .profile(ProfileResponse.fromEntity(profile))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp, HttpServletResponse response) {
        // 1. Rate-limit login
        rateLimiterService.checkRateLimit("auth:login:" + clientIp, loginMaxRequests, loginWindowSeconds, "login attempts");

        // 2. Normalize email
        String email = normalizeEmail(request.getEmail());

        // 3. Look up user by email
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("Login failed: account not found for email: {}", email);
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userOpt.get();

        // 4. Verify password hash - generic error message for bad credentials
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password for user: {}", user.getId());
            throw new UnauthorizedException("Invalid email or password");
        }

        // 5. Check user status - Suspended / deleted users cannot sign in
        if (user.getStatus() == UserStatus.SUSPENDED) {
            log.warn("Login blocked: user {} is SUSPENDED", user.getId());
            throw new ForbiddenException("Account is suspended");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            log.warn("Login blocked: user {} is DELETED", user.getId());
            throw new ForbiddenException("Account has been deleted");
        }

        // 6. Create secure session and set HttpOnly + Secure + SameSite cookie
        String sessionId = createSession(user.getId());
        attachSessionCookie(response, sessionId, sessionDurationSeconds);
        log.info("User {} successfully signed in, new session created", user.getId());

        // 7. Retrieve user profile (or create an empty one if missing)
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultProfile(user.getId()));

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .profile(ProfileResponse.fromEntity(profile))
                .build();
    }

    @Override
    @Transactional
    public void logout(String token, String currentUserId, HttpServletResponse response) {
        // End session in database
        if (StringUtils.hasText(token)) {
            sessionRepository.deleteByToken(token);
            log.info("Terminated session by token");
        } else if (StringUtils.hasText(currentUserId)) {
            sessionRepository.deleteByUserId(currentUserId);
            log.info("Terminated sessions for user id: {}", currentUserId);
        }

        // Clear session cookie
        attachSessionCookie(response, "", 0);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse getCurrentUser(String currentUserId) {
        if (!StringUtils.hasText(currentUserId)) {
            throw new UnauthorizedException("User session/authentication is missing");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ForbiddenException("Account is suspended");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ForbiddenException("Account has been deleted");
        }

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultProfile(user.getId()));

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .profile(ProfileResponse.fromEntity(profile))
                .build();
    }

    private String createSession(String userId) {
        String token = UUID.randomUUID().toString().replace("-", "") +
                       UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plusSeconds(sessionDurationSeconds);

        UserSession session = new UserSession(
                IdGenerator.nextId(),
                token,
                userId,
                expiresAt,
                Instant.now()
        );
        sessionRepository.save(session);
        return token;
    }

    private void attachSessionCookie(HttpServletResponse response, String token, long maxAge) {
        if (response == null) {
            return;
        }

        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private Profile createDefaultProfile(String userId) {
        Profile profile = Profile.builder()
                .id(IdGenerator.nextId())
                .userId(userId)
                .build();
        return profileRepository.save(profile);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new UnauthorizedException("Email cannot be blank");
        }
        return email.trim().toLowerCase();
    }
}
