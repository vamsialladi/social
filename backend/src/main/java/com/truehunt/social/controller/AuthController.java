package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.repository.ProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ProfileRepository profileRepository;

    public AuthController(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
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
        String email = credentials.get("email");
        Optional<ProfileEntity> profile = profileRepository.findByUserId("me");

        Map<String, Object> authData = new HashMap<>();
        authData.put("token", "truehunt-session-" + UUID.randomUUID());
        authData.put("userId", "me");
        authData.put("email", email != null ? email : (profile.map(ProfileEntity::getEmail).orElse("user@truehunt.com")));
        profile.ifPresent(p -> {
            authData.put("profile", p.toDto());
            authData.put("user", Map.of(
                "id", "me",
                "name", p.getName() != null ? p.getName() : "Cinema Professional",
                "email", p.getEmail() != null ? p.getEmail() : email,
                "role", "TALENT"
            ));
        });

        return ResponseEntity.ok(ApiResponse.success(authData));
    }

    @PostMapping("/register")
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
        String role = data.get("role") != null ? String.valueOf(data.get("role")) : "TALENT";

        ProfileEntity profile = profileRepository.findByUserId("me").orElseGet(() -> {
            ProfileEntity p = new ProfileEntity();
            p.setUserId("me");
            return p;
        });

        if (name != null && !name.isBlank()) profile.setName(name);
        if (email != null && !email.isBlank()) profile.setEmail(email);
        if (phone != null && !phone.isBlank()) profile.setPhone(phone);
        if (experience != null && !experience.isBlank()) profile.setExperience(experience);

        if (headquarters != null && !headquarters.isBlank()) {
            profile.setAddress(headquarters);
        } else if (profile.getAddress() == null || profile.getAddress().isBlank()) {
            profile.setAddress("Hyderabad, India");
        }

        if (craft != null && !craft.isBlank()) {
            List<String> currentTalents = new ArrayList<>(profile.getTalents());
            if (!currentTalents.contains(craft)) {
                currentTalents.add(0, craft);
            }
            profile.setTalents(currentTalents);
        }

        if (profile.getLanguages().isEmpty()) {
            profile.setLanguages(List.of("English", "Telugu"));
        }
        if (profile.getWorkTypes().isEmpty()) {
            profile.setWorkTypes(List.of("Cinema"));
        }

        profile.setUpdatedAt(LocalDateTime.now());
        ProfileEntity saved = profileRepository.save(profile);

        Map<String, Object> authData = new HashMap<>();
        String token = "truehunt-session-" + UUID.randomUUID();
        authData.put("token", token);
        authData.put("userId", "me");
        authData.put("role", role);
        authData.put("profile", saved.toDto());
        authData.put("user", Map.of(
                "id", "me",
                "name", saved.getName() != null ? saved.getName() : (name != null ? name : "Cinema Professional"),
                "email", saved.getEmail() != null ? saved.getEmail() : (email != null ? email : ""),
                "role", role
        ));

        return ResponseEntity.ok(ApiResponse.success(authData));
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
}
