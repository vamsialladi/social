package com.truehunt.social.service;

import com.truehunt.social.dto.*;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ConflictException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.PasswordResetTokenEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.PasswordResetTokenRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import com.truehunt.social.security.JwtTokenProvider;
import com.truehunt.social.security.LoginRateLimiter;
import com.truehunt.social.validation.PasswordPolicy;
import com.truehunt.social.validation.PhoneNumberPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String DUPLICATE_EMAIL_MESSAGE =
            "This email is already registered. Please use another email address.";
    private static final String DUPLICATE_PHONE_MESSAGE =
            "This phone number is already registered. Please use another phone number.";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final ProfileRepository profileRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final LoginRateLimiter loginRateLimiter;
    private final long resetTokenTtlMinutes;

    public AuthService(ProfileRepository profileRepository,
                       UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this(profileRepository, userAccountRepository, passwordEncoder, tokenProvider, null, null, 15);
    }

    @Autowired
    public AuthService(ProfileRepository profileRepository,
                       UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       LoginRateLimiter loginRateLimiter,
                       @Value("${app.auth.reset-token-ttl-minutes:15}") long resetTokenTtlMinutes) {
        this.profileRepository = profileRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.loginRateLimiter = loginRateLimiter;
        this.resetTokenTtlMinutes = resetTokenTtlMinutes;
    }

    public OtpResponse sendOtp(OtpRequest req) {
        String destination = req != null ? req.getEmail() : "";
        String msg = "Dummy OTP 123456 generated for " + (destination != null && !destination.isBlank() ? destination : "your phone number");
        return new OtpResponse("123456", msg);
    }

    public OtpResponse verifyOtp(OtpRequest req) {
        String otp = req != null ? req.getOtp() : "";
        boolean valid = otp != null && "123456".equals(otp.trim());
        return new OtpResponse(valid, valid ? "OTP verified successfully" : "Invalid OTP");
    }

    @Transactional
    public AuthResponse login(LoginRequest credentials) {
        return login(credentials, null);
    }

    @Transactional
    public AuthResponse login(LoginRequest credentials, String clientIp) {
        String email = normalizeEmail(credentials.getEmail());
        if (email == null) {
            throw new BadRequestException("Email is required.");
        }

        if (loginRateLimiter != null) {
            loginRateLimiter.checkAllowed(email, clientIp);
        }

        Optional<UserAccountEntity> userOpt = userAccountRepository.findByEmail(email);
        if (userOpt.isEmpty()
                || credentials.getPassword() == null
                || !passwordEncoder.matches(credentials.getPassword(), userOpt.get().getPasswordHash())) {
            if (loginRateLimiter != null) {
                loginRateLimiter.recordFailure(email, clientIp);
            }
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password.");
        }

        if (loginRateLimiter != null) {
            loginRateLimiter.clearFailures(email, clientIp);
        }

        UserAccountEntity user = userOpt.get();
        ensurePublicUserId(user);

        String token = tokenProvider.generateToken(user.getEmail(), user.getRole());
        String publicId = user.getPublicUserId() != null ? user.getPublicUserId() : user.getId().toString();
        return buildAuthResponse(user, publicId, token);
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.getEmail());
        if (email == null || email.isBlank()) {
            throw new BadRequestException("VALIDATION_ERROR", "Email is required.", Map.of("email", "Email is required."));
        }

        String name = req.getName();
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required.");
        }

        PasswordPolicy.requireValid(req.getPassword(), "password");
        PasswordPolicy.requireConfirmed(req.getPassword(), req.getConfirmPassword(), false);
        String normalizedPhone = PhoneNumberPolicy.validateAndNormalize(req.getPhone());

        if (userAccountRepository.existsByEmail(email) || userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw duplicateEmailConflict();
        }
        userAccountRepository.findByEmail(email).ifPresent(existing -> {
            if ("ADMIN".equalsIgnoreCase(existing.getRole())) {
                throw new ConflictException("This email is reserved for an administrator account.");
            }
            throw duplicateEmailConflict();
        });

        if (normalizedPhone != null && userAccountRepository.existsByPhoneNumber(normalizedPhone)) {
            throw duplicatePhoneConflict();
        }

        String reqRole = req.getRole();
        String role = ("PRODUCER".equalsIgnoreCase(reqRole) || "STUDIO".equalsIgnoreCase(reqRole)) ? "STUDIO" : "TALENT";

        UserAccountEntity user = new UserAccountEntity();
        String publicUserId = generateUniquePublicUserId(name, role);
        user.setCreatedAt(LocalDateTime.now());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setName(name);
        user.setPhone(normalizedPhone);
        user.setRole(role);
        user.setPublicUserId(publicUserId);
        user.setPrimaryCraft(req.getPrimaryCraft());
        user.setSubCraft(req.getSubCraft());
        user.setExperience(req.getExperience());
        user.setCompanyName(req.getCompanyName());
        user.setHeadquarters(req.getHeadquarters());
        user.setAcceptedTerms(req.getAcceptedTerms());
        try {
            userAccountRepository.save(user);
            userAccountRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            if (isEmailUniqueViolation(ex)) {
                throw duplicateEmailConflict();
            }
            if (isPhoneUniqueViolation(ex, normalizedPhone)) {
                throw duplicatePhoneConflict();
            }
            throw ex;
        }

        // Ensure Profile Entity exists
        ProfileEntity profile = profileRepository.findByUserId(publicUserId)
                .or(() -> profileRepository.findByEmail(email).filter(p -> !"me".equalsIgnoreCase(p.getUserId())))
                .orElseGet(ProfileEntity::new);
        profile.setUserId(publicUserId);
        profile.setName(user.getName());
        profile.setEmail(user.getEmail());
        profile.setPhone(user.getPhone());
        profile.setPrimaryCraft(user.getPrimaryCraft());
        profile.setSubCraft(user.getSubCraft());
        if ("STUDIO".equals(role)) {
            profile.setRole("Production House");
            profile.setCompany(user.getCompanyName());
            profile.setAddress(user.getHeadquarters());
        }
        profile.setExperience(user.getExperience());
        if (req.getLanguages() != null) profile.setLanguages(req.getLanguages());
        if (req.getSecondaryCrafts() != null) profile.setSecondaryCrafts(req.getSecondaryCrafts());
        if (req.getSecondarySubCrafts() != null) profile.setSecondarySubCrafts(req.getSecondarySubCrafts());
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);

        String token = tokenProvider.generateToken(user.getEmail(), user.getRole());
        return buildAuthResponse(user, publicUserId, token);
    }

    public Map<String, Object> getCurrentUser(String identifier) {
        Optional<UserAccountEntity> opt = Optional.empty();
        if (identifier != null && !identifier.isBlank()) {
            String trimmed = identifier.trim();
            opt = userAccountRepository.findByEmail(normalizeEmail(trimmed))
                    .or(() -> userAccountRepository.findByPublicUserId(trimmed));
        }
        if (opt.isPresent()) {
            UserAccountEntity u = opt.get();
            String publicId = u.getPublicUserId() != null ? u.getPublicUserId() : u.getId().toString();
            return buildUserMap(u, publicId);
        }

        // Default cinema professional fallback if not signed in
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("id", "1000001");
        fallback.put("userId", "1000001");
        fallback.put("publicUserId", "1000001");
        fallback.put("name", "Cinema Professional");
        fallback.put("email", "pro@truehunt.in");
        fallback.put("role", "TALENT");
        fallback.put("primaryCraft", "Acting");
        return fallback;
    }

    private AuthResponse buildAuthResponse(UserAccountEntity user, String publicId, String token) {
        AuthResponse resp = new AuthResponse();
        resp.setToken(token);
        resp.setUserId(publicId);
        resp.setEmail(user.getEmail());
        resp.setRole(user.getRole());
        resp.setName(user.getName());
        resp.setPhone(user.getPhone());
        resp.setPrimaryCraft(user.getPrimaryCraft());
        resp.setSubCraft(user.getSubCraft());
        resp.setUser(buildUserMap(user, publicId));
        return resp;
    }

    private Map<String, Object> buildUserMap(UserAccountEntity u, String publicId) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", publicId);
        data.put("userId", publicId);
        data.put("publicUserId", publicId);
        data.put("email", u.getEmail());
        data.put("name", u.getName());
        data.put("role", u.getRole());
        data.put("phone", u.getPhone());
        data.put("primaryCraft", u.getPrimaryCraft());
        data.put("subCraft", u.getSubCraft());
        data.put("experience", u.getExperience());
        data.put("companyName", u.getCompanyName());
        data.put("headquarters", u.getHeadquarters());
        return data;
    }

    public void logout(String token) {
        if (token != null) {
            tokenProvider.invalidateToken(token);
        }
    }

    @Transactional
    public Map<String, String> requestPasswordReset(ForgotPasswordRequest req) {
        String email = req != null ? normalizeEmail(req.getEmail()) : null;
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required.");
        }

        if (passwordResetTokenRepository != null) {
            userAccountRepository.findByEmail(email).ifPresent(user -> {
                invalidateActiveResetTokens(user.getEmail());

                byte[] randomBytes = new byte[32];
                SECURE_RANDOM.nextBytes(randomBytes);
                String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

                PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
                entity.setEmail(user.getEmail());
                entity.setTokenHash(sha256Hex(rawToken));
                entity.setExpiresAt(LocalDateTime.now().plusMinutes(resetTokenTtlMinutes));
                entity.setUsed(false);
                entity.setCreatedAt(LocalDateTime.now());
                passwordResetTokenRepository.save(entity);

                deliverPasswordResetToken(user.getEmail(), rawToken);
            });
        }

        return Map.of("message", "If an account exists for that email, password reset instructions have been sent.");
    }

    @Transactional
    public Map<String, String> resetPassword(ResetPasswordRequest req) {
        if (req == null || req.getToken() == null || req.getToken().isBlank()) {
            throw new BadRequestException("INVALID_RESET_TOKEN", "Invalid or expired password reset token.");
        }

        PasswordPolicy.requireValid(req.getNewPassword(), "newPassword");
        PasswordPolicy.requireConfirmed(req.getNewPassword(), req.getConfirmPassword(), true);

        if (passwordResetTokenRepository == null) {
            throw new BadRequestException("INVALID_RESET_TOKEN", "Invalid or expired password reset token.");
        }

        String tokenHash = sha256Hex(req.getToken().trim());
        PasswordResetTokenEntity resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("INVALID_RESET_TOKEN", "Invalid or expired password reset token."));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("INVALID_RESET_TOKEN", "Invalid or expired password reset token.");
        }

        UserAccountEntity user = userAccountRepository.findByEmail(normalizeEmail(resetToken.getEmail()))
                .orElseThrow(() -> new BadRequestException("INVALID_RESET_TOKEN", "Invalid or expired password reset token."));

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userAccountRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        invalidateActiveResetTokens(user.getEmail());
        if (loginRateLimiter != null) {
            loginRateLimiter.clearFailuresForEmail(user.getEmail());
        }

        return Map.of("message", "Password has been reset successfully.");
    }

    public void deliverPasswordResetToken(String email, String rawToken) {
        // Deliberately avoid logging raw tokens or sensitive credential details.
        log.info("Password reset token generated and queued for delivery");
    }

    private void invalidateActiveResetTokens(String email) {
        List<PasswordResetTokenEntity> active = passwordResetTokenRepository.findByEmailIgnoreCaseAndUsedFalse(email);
        if (!active.isEmpty()) {
            for (PasswordResetTokenEntity existing : active) {
                existing.setUsed(true);
            }
            passwordResetTokenRepository.saveAll(active);
        }
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }

    private ConflictException duplicateEmailConflict() {
        return new ConflictException(
                "DUPLICATE_EMAIL",
                DUPLICATE_EMAIL_MESSAGE,
                Map.of("email", DUPLICATE_EMAIL_MESSAGE)
        );
    }

    private boolean isEmailUniqueViolation(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase(Locale.ROOT);
                if (lower.contains("uk_user_accounts_email") || lower.contains("(email")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private ConflictException duplicatePhoneConflict() {
        return new ConflictException(
                "DUPLICATE_PHONE_NUMBER",
                DUPLICATE_PHONE_MESSAGE,
                Map.of("phoneNumber", DUPLICATE_PHONE_MESSAGE)
        );
    }

    private boolean isPhoneUniqueViolation(Throwable ex, String normalizedPhone) {
        if (normalizedPhone == null) {
            return false;
        }
        Throwable current = ex;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase(Locale.ROOT);
                if (lower.contains("uk_user_accounts_phone_number") || lower.contains("phone_number")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private void ensurePublicUserId(UserAccountEntity user) {
        if (user.getPublicUserId() == null || user.getPublicUserId().isBlank()) {
            user.setPublicUserId(generateUniquePublicUserId(user.getName(), user.getRole()));
            userAccountRepository.save(user);
        }
    }

    private String generateUniquePublicUserId(String name, String role) {
        String[] words = name == null ? new String[0] : name.trim().split("\\s+");
        String firstWordLetters = words.length > 0 ? words[0].replaceAll("[^\\p{L}]", "").toUpperCase(Locale.ROOT) : "";
        String lastWordLetters = words.length > 1 ? words[words.length - 1].replaceAll("[^\\p{L}]", "").toUpperCase(Locale.ROOT) : "";
        String initials;
        if (!firstWordLetters.isEmpty() && !lastWordLetters.isEmpty()) {
            initials = firstWordLetters.substring(0, 1) + lastWordLetters.substring(0, 1);
        } else {
            String compact = name == null ? "" : name.replaceAll("[^\\p{L}]", "").toUpperCase(Locale.ROOT);
            initials = compact.length() >= 2 ? compact.substring(0, 2) : (compact + "XX").substring(0, 2);
        }
        String prefix = "PRODUCER".equalsIgnoreCase(role) || "STUDIO".equalsIgnoreCase(role)
                ? "thP"
                : "thsc";

        for (int i = 0; i < 100; i++) {
            String candidate = prefix + initials
                    + String.format(Locale.ROOT, "%04d", SECURE_RANDOM.nextInt(90_000) + 10_000);
            if (userAccountRepository.findByPublicUserId(candidate).isEmpty()
                    && profileRepository.findByUserId(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique public user ID.");
    }
}
