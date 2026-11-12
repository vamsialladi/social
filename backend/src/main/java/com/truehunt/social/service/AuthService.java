package com.truehunt.social.service;

import com.truehunt.social.dto.*;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ConflictException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import com.truehunt.social.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final ProfileRepository profileRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(ProfileRepository profileRepository,
                       UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.profileRepository = profileRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
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
        String email = normalizeEmail(credentials.getEmail());
        if (email == null) {
            throw new BadRequestException("Email is required.");
        }

        UserAccountEntity user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password."));

        if (credentials.getPassword() == null || !passwordEncoder.matches(credentials.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password.");
        }

        ensurePublicUserId(user);

        String token = tokenProvider.generateToken(user.getEmail(), user.getRole());
        String publicId = user.getPublicUserId() != null ? user.getPublicUserId() : user.getId().toString();
        return buildAuthResponse(user, publicId, token);
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.getEmail());
        if (email == null) {
            throw new BadRequestException("Valid email is required.");
        }

        String name = req.getName();
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required.");
        }

        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new BadRequestException("Password is required.");
        }

        String reqRole = req.getRole();
        String role = ("PRODUCER".equalsIgnoreCase(reqRole) || "STUDIO".equalsIgnoreCase(reqRole)) ? "STUDIO" : "TALENT";

        Optional<UserAccountEntity> existingOpt = userAccountRepository.findByEmail(email);
        UserAccountEntity user;
        String publicUserId;

        if (existingOpt.isPresent()) {
            user = existingOpt.get();
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                throw new ConflictException("This email is reserved for an administrator account.");
            }
            publicUserId = user.getPublicUserId() != null
                    ? user.getPublicUserId()
                    : generateUniquePublicUserId(name, role);
        } else {
            user = new UserAccountEntity();
            publicUserId = generateUniquePublicUserId(name, role);
            user.setCreatedAt(LocalDateTime.now());
        }

        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setName(name);
        user.setPhone(req.getPhone());
        user.setRole(role);
        user.setPublicUserId(publicUserId);
        user.setPrimaryCraft(req.getPrimaryCraft());
        user.setSubCraft(req.getSubCraft());
        user.setExperience(req.getExperience());
        user.setCompanyName(req.getCompanyName());
        user.setHeadquarters(req.getHeadquarters());
        user.setAcceptedTerms(req.getAcceptedTerms());
        userAccountRepository.save(user);

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
