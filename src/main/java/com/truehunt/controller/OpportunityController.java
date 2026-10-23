package com.truehunt.controller;

import com.truehunt.config.UserPrincipal;
import com.truehunt.dto.request.CreateOpportunityRequest;
import com.truehunt.dto.response.ApiResponse;
import com.truehunt.dto.response.PostResponse;
import com.truehunt.service.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/opportunities")
public class OpportunityController {

    @Autowired
    private PostService postService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponse>>> getOpportunitiesFeed(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {

        List<PostResponse> opportunities = postService.getOpportunitiesFeed(cursor, limit);
        String nextCursor = null;
        if (!opportunities.isEmpty() && opportunities.size() >= limit) {
            nextCursor = opportunities.get(opportunities.size() - 1).getCreatedAt().toString();
        }

        return ResponseEntity.ok(ApiResponse.of(opportunities, nextCursor));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createOpportunity(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateOpportunityRequest request) {

        PostResponse created = postService.createOpportunity(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }
}
