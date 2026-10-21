package com.social.controller;

import com.social.config.UserContextHolder;
import com.social.dto.request.CreatePostRequest;
import com.social.dto.response.ApiResponse;
import com.social.dto.response.CursorPageResponse;
import com.social.dto.response.OpportunityResponse;
import com.social.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Module 4: Posts Feed", description = "Endpoints for ordinary social posts feed (excluding opportunities)")
public class PostController {

    private final OpportunityService opportunityService;

    @GetMapping
    @Operation(summary = "Get ordinary posts feed", description = "Returns ordinary social feed posts. Enforces rule: Opportunity posts must not appear in the ordinary posts feed.")
    public ResponseEntity<CursorPageResponse<OpportunityResponse>> getPostsFeed(
            @Parameter(description = "Pagination cursor (ISO-8601 timestamp string of last item)")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "Limit number of items per page (default: 20)")
            @RequestParam(defaultValue = "20") int limit
    ) {
        CursorPageResponse<OpportunityResponse> page = opportunityService.getOrdinaryPostsFeed(cursor, limit);
        return ResponseEntity.ok(page);
    }

    @PostMapping
    @Operation(summary = "Create an ordinary post", description = "Create a standard social post with type = post")
    public ResponseEntity<ApiResponse<OpportunityResponse>> createPost(
            @Valid @RequestBody CreatePostRequest request
    ) {
        String currentUserId = UserContextHolder.requireUserId();
        OpportunityResponse created = opportunityService.createPost(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post created successfully", created));
    }
}
