package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final NotificationRepository notificationRepository;

    public PromotionController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @PostMapping("/click")
    public ResponseEntity<ApiResponse<Map<String, Object>>> clickPromotion(@RequestBody(required = false) Map<String, String> body) {
        String campaign = body != null && body.containsKey("campaign") ? body.get("campaign") : "Northstar Films Career Promotion";

        notificationRepository.save(new NotificationEntity(
                "me",
                "Studio Inquiry Sent",
                "Your interest in film careers with Northstar Films has been recorded.",
                "promotion"
        ));

        Map<String, Object> res = new HashMap<>();
        res.put("campaign", campaign);
        res.put("status", "SUCCESS");
        res.put("message", "Application registered with studio casting office.");
        return ResponseEntity.ok(ApiResponse.success(res));
    }
}
