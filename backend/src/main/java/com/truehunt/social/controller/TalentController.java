package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.TalentDto;
import com.truehunt.social.service.TalentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/talents")
public class TalentController {

    private final TalentService talentService;

    public TalentController(TalentService talentService) {
        this.talentService = talentService;
    }

    private String resolveCaller(String userEmail, String userId, Authentication authentication) {
        if (userEmail != null && !userEmail.isBlank()) return userEmail;
        if (userId != null && !userId.isBlank()) return userId;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TalentDto>>> getAllTalents(
            @RequestParam(required = false) Map<String, String> params,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        List<TalentDto> list = talentService.getAll(params, resolveCaller(userEmail, userId, authentication));
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TalentDto>> getTalentById(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        TalentDto talent = talentService.getById(id, resolveCaller(userEmail, userId, authentication));
        return ResponseEntity.ok(ApiResponse.success(talent));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TalentDto>>> searchTalents(
            @RequestParam(required = false) Map<String, String> params,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Authentication authentication
    ) {
        List<TalentDto> list = talentService.getAll(params, resolveCaller(userEmail, userId, authentication));
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
