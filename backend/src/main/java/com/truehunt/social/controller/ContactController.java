package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.model.ContactMessageEntity;
import com.truehunt.social.model.LcmBetaSignupEntity;
import com.truehunt.social.repository.ContactMessageRepository;
import com.truehunt.social.repository.LcmBetaSignupRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ContactController {

    private final ContactMessageRepository contactMessageRepository;
    private final LcmBetaSignupRepository lcmBetaSignupRepository;

    public ContactController(
            ContactMessageRepository contactMessageRepository,
            LcmBetaSignupRepository lcmBetaSignupRepository
    ) {
        this.contactMessageRepository = contactMessageRepository;
        this.lcmBetaSignupRepository = lcmBetaSignupRepository;
    }

    @PostMapping("/contact")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitContact(
            @RequestBody(required = false) Map<String, Object> req
    ) {
        if (req == null) {
            throw new BadRequestException("Contact details are required.");
        }
        String fullName = firstNonBlank(req, "fullName", "name");
        String email = firstNonBlank(req, "email");
        String phone = firstNonBlank(req, "phone", "phoneNumber");
        String subject = firstNonBlank(req, "subject");
        String message = firstNonBlank(req, "message", "description");

        if (fullName.isEmpty() || email.isEmpty() || subject.isEmpty() || message.isEmpty()) {
            throw new BadRequestException("Full name, email, subject, and message are required.");
        }

        ContactMessageEntity entity = new ContactMessageEntity();
        entity.setId("CNT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        entity.setFullName(fullName);
        entity.setEmail(email);
        entity.setPhone(phone);
        entity.setSubject(subject);
        entity.setMessage(message);
        entity.setStatus("RECEIVED");
        entity.setCreatedAt(LocalDateTime.now());
        ContactMessageEntity saved = contactMessageRepository.save(entity);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", saved.getId());
        res.put("status", saved.getStatus());
        res.put("message", "Your message has been received. Our team will get back to you shortly.");
        res.put("createdAt", saved.getCreatedAt().toString());
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @PostMapping("/lcm/beta-signup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> joinLcmBeta(
            @RequestBody(required = false) Map<String, Object> req
    ) {
        String email = req == null ? "" : firstNonBlank(req, "email", "betaEmail");
        if (email.isEmpty() || !email.contains("@")) {
            throw new BadRequestException("A valid email address is required.");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Optional<LcmBetaSignupEntity> existing = lcmBetaSignupRepository.findByEmailIgnoreCase(normalizedEmail);
        LcmBetaSignupEntity saved = existing.orElseGet(() -> lcmBetaSignupRepository.save(new LcmBetaSignupEntity(normalizedEmail)));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", saved.getId());
        res.put("email", saved.getEmail());
        res.put("status", saved.getStatus());
        res.put("alreadyRegistered", existing.isPresent());
        res.put("message", existing.isPresent()
                ? "This email is already registered for early beta access."
                : "You're on the beta waitlist! Early access details will be emailed to you.");
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    private String firstNonBlank(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) {
                String str = String.valueOf(val).trim();
                if (!str.isEmpty()) {
                    return str;
                }
            }
        }
        return "";
    }
}
