package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ProfileRepository profileRepository;
    private final UserAccountRepository userAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(ProfileRepository profileRepository, UserAccountRepository userAccountRepository) {
        this.profileRepository = profileRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendOtp(@RequestBody(required = false) Map<String, String> req) {
        String destination = req != null ? req.getOrDefault("email", req.getOrDefault("destination", "")) : "";
        Map<String, String> res = new HashMap<>();
        res.put("otp", "123456");
        res.put("message", "Dummy OTP 123456 sent successfully to " + (!destination.isBlank() ? destination : "your email"));
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(@RequestBody(required = false) Map<String, String> req) {
        String otp = req != null ? req.get("otp") : "";
        boolean valid = otp != null && (!otp.isBlank() || otp.equals("123456"));
        Map<String, Object> res = new HashMap<>();
        res.put("valid", valid);
        res.put("message", valid ? "OTP verified successfully" : "Invalid OTP");
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> credentials) {
        String email = normalizeEmail(credentials.get("email"));
        String password = credentials.get("password");
        Optional<UserAccountEntity> account = email == null
                ? Optional.empty()
                : userAccountRepository.findByEmail(email);

        if (account.isEmpty() || password == null
                || !passwordEncoder.matches(password, account.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("INVALID_CREDENTIALS", "Invalid email or password."));
        }

        UserAccountEntity user = account.get();
        String requestedRole = credentials.get("role");
        if (requestedRole != null && !requestedRole.isBlank()
                && !user.getRole().equalsIgnoreCase(requestedRole)) {
            String accountType = "TALENT".equals(user.getRole()) ? "Social" : "Producer";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("ACCOUNT_TYPE_MISMATCH",
                            "This account is registered as " + accountType + ". Select that account type to sign in."));
        }

        Map<String, Object> authData = new HashMap<>();
        authData.put("token", "truehunt-session-" + UUID.randomUUID());
        authData.put("userId", user.getId().toString());
        authData.put("email", user.getEmail());
        authData.put("role", user.getRole());
        authData.put("user", Map.of(
                "id", user.getId().toString(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole()
        ));
        return ResponseEntity.ok(ApiResponse.success(authData));
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody Map<String, Object> data) {
        String email = data.get("email") != null ? String.valueOf(data.get("email")) : null;
        String name = data.get("fullName") != null ? String.valueOf(data.get("fullName"))
                : (data.get("name") != null ? String.valueOf(data.get("name")) : null);
        String phone = data.get("phoneNumber") != null ? String.valueOf(data.get("phoneNumber"))
                : (data.get("phone") != null ? String.valueOf(data.get("phone")) : null);
        String craft = data.get("craft") != null ? String.valueOf(data.get("craft")) : null;
        String experience = data.get("experienceYears") != null ? String.valueOf(data.get("experienceYears"))
                : (data.get("experience") != null ? String.valueOf(data.get("experience")) : null);
        String companyName = data.get("companyName") != null ? String.valueOf(data.get("companyName")) : null;
        String headquarters = data.get("headquarters") != null ? String.valueOf(data.get("headquarters"))
                : (data.get("address") != null ? String.valueOf(data.get("address")) : null);
        String password = data.get("password") != null ? String.valueOf(data.get("password")) : null;
        String role = data.get("role") != null ? String.valueOf(data.get("role")).toUpperCase(Locale.ROOT) : "TALENT";
        email = normalizeEmail(email);

        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_EMAIL", "Enter a valid email address."));
        }
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_PASSWORD", "Password is required."));
        }
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_PASSWORD", "Password must be 72 bytes or fewer."));
        }
        if (!"TALENT".equals(role) && !"STUDIO".equals(role)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_ROLE", "Choose Social or Producer."));
        }
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_NAME", "Your name is required."));
        }
        if (userAccountRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("EMAIL_ALREADY_REGISTERED", "An account with this email already exists."));
        }

        UserAccountEntity account = new UserAccountEntity();
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setName(name.trim());
        account.setPhoneNumber(phone);
        account.setRole(role);
        account.setCraft(craft);
        account.setExperienceYears(experience);
        account.setCompanyName(companyName);
        account.setHeadquarters(headquarters);
        UserAccountEntity savedAccount = userAccountRepository.save(account);

        Optional<ProfileEntity> savedProfile = Optional.empty();
        if ("TALENT".equals(role)) {
            ProfileEntity profile = profileRepository.findByUserId("me").orElseGet(() -> {
                ProfileEntity p = new ProfileEntity();
                p.setUserId("me");
                return p;
            });

            profile.setName(name.trim());
            profile.setEmail(email);
            if (phone != null && !phone.isBlank()) profile.setPhone(phone);
            if (experience != null && !experience.isBlank()) profile.setExperience(experience);
            if (profile.getAddress() == null || profile.getAddress().isBlank()) {
                profile.setAddress("Hyderabad, India");
            }
            if (craft != null && !craft.isBlank()) {
                List<String> currentTalents = new ArrayList<>(profile.getTalents());
                if (!currentTalents.contains(craft)) currentTalents.add(0, craft);
                profile.setTalents(currentTalents);
            }
            if (profile.getLanguages().isEmpty()) profile.setLanguages(List.of("English", "Telugu"));
            if (profile.getWorkTypes().isEmpty()) profile.setWorkTypes(List.of("Cinema"));
            profile.setUpdatedAt(LocalDateTime.now());
            savedProfile = Optional.of(profileRepository.save(profile));
        }

        Map<String, Object> authData = new HashMap<>();
        String token = "truehunt-session-" + UUID.randomUUID();
        authData.put("token", token);
        authData.put("userId", savedAccount.getId().toString());
        authData.put("role", role);
        savedProfile.ifPresent(profile -> authData.put("profile", profile.toDto()));
        authData.put("user", Map.of(
                "id", savedAccount.getId().toString(),
                "name", savedAccount.getName(),
                "email", savedAccount.getEmail(),
                "role", role
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(authData));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me() {
        Optional<ProfileEntity> profile = profileRepository.findByUserId("me");
        Map<String, Object> user = new HashMap<>();
        user.put("userId", "me");
        user.put("email", profile.map(ProfileEntity::getEmail).orElse("user@truehunt.com"));
        profile.ifPresent(p -> {
            user.put("profile", p.toDto());
            user.put("name", p.getName());
            user.put("role", "TALENT");
        });
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
