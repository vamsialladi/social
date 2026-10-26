package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class SupportController {

    private static final List<Map<String, String>> FAQS = List.of(
            Map.of(
                    "id", "faq-1",
                    "question", "What is TrueHunt Cinema Network?",
                    "answer", "TrueHunt is the professional cinema and entertainment platform designed exclusively for verified talents, technicians, directors, producers, and investors to collaborate transparently.",
                    "category", "Platform"
            ),
            Map.of(
                    "id", "faq-2",
                    "question", "How do I get my professional profile verified?",
                    "answer", "You can submit verified call sheets, theatrical or OTT credits, or an industry reference ID. Our verification board validates credits within 48 hours to grant your verification checkmark.",
                    "category", "Verification"
            ),
            Map.of(
                    "id", "faq-3",
                    "question", "What is Protect Producer?",
                    "answer", "Protect Producer is TrueHunt's comprehensive project tracking, legal documentation lock, and milestone oversight system for movie productions, web series, short films, and serials.",
                    "category", "Protect Producer"
            ),
            Map.of(
                    "id", "faq-4",
                    "question", "Are casting auditions verified?",
                    "answer", "Yes. All production houses and casting directors publishing auditions on TrueHunt are authenticated to ensure safe and legitimate industry opportunities.",
                    "category", "Auditions & Jobs"
            )
    );

    @GetMapping("/faqs")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getFaqs() {
        return ResponseEntity.ok(ApiResponse.success(FAQS));
    }

    @PostMapping("/support")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitTicket(@RequestBody(required = false) Map<String, Object> ticket) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("ticketId", "ticket-" + System.currentTimeMillis());
        res.put("message", "Your inquiry has been logged. Our TrueHunt industry support team will respond within 24 hours.");
        res.put("submittedAt", new Date().toString());
        return ResponseEntity.ok(ApiResponse.success(res));
    }
}
