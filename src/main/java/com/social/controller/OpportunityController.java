package com.social.controller;

import com.social.config.UserContextHolder;
import com.social.dto.request.CreateOpportunityRequest;
import com.social.dto.response.ApiResponse;
import com.social.dto.response.CursorPageResponse;
import com.social.dto.response.OpportunityResponse;
import com.social.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/opportunities")
@Tag(name = "Module 5: Opportunities", description = "Endpoints for job and casting opportunities posted by users")
public class OpportunityController {

    private final OpportunityService opportunityService;

    public OpportunityController(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @GetMapping
    @Operation(summary = "Get opportunities", description = "Return only records with type = opportunity, paginated and newest first")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Opportunities retrieved successfully")
    })
    public ResponseEntity<CursorPageResponse<OpportunityResponse>> getOpportunities(
            @Parameter(description = "Pagination cursor (ISO-8601 timestamp string of last item)")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "Limit number of items per page (default: 20)")
            @RequestParam(defaultValue = "20") int limit
    ) {
        CursorPageResponse<OpportunityResponse> page = opportunityService.getOpportunities(cursor, limit);
        return ResponseEntity.ok(page);
    }

    @PostMapping
    @Operation(summary = "Create an opportunity", description = "Create an opportunity as the signed-in user. Server forces type = opportunity, sets author and timestamp, validates, saves.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Opportunity created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required (missing X-User-Id header)")
    })
    public ResponseEntity<ApiResponse<OpportunityResponse>> createOpportunity(
            @Valid @RequestBody CreateOpportunityRequest request
    ) {
        String currentUserId = UserContextHolder.requireUserId();
        OpportunityResponse created = opportunityService.createOpportunity(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Opportunity created successfully", created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get opportunity by ID", description = "Retrieve a single opportunity post by its identifier")
    public ResponseEntity<ApiResponse<OpportunityResponse>> getOpportunityById(@PathVariable String id) {
        OpportunityResponse opportunity = opportunityService.getOpportunityById(id);
        return ResponseEntity.ok(ApiResponse.success(opportunity));
    }
}
