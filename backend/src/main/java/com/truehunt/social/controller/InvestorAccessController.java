package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.InvestorAccessRequestDto;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.service.InvestorAccessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/investor-access")
public class InvestorAccessController {

    private final InvestorAccessService investorAccessService;

    public InvestorAccessController(InvestorAccessService investorAccessService) {
        this.investorAccessService = investorAccessService;
    }

    private String resolveCaller(UserDetails user, String userEmail, String userId) {
        if (user != null && user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        if (userEmail != null && !userEmail.isBlank()) {
            return userEmail.trim();
        }
        if (userId != null && !userId.isBlank()) {
            return userId.trim();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        throw new UnauthorizedException("Sign in required for investor access.");
    }

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submit(
            @Valid @RequestBody InvestorAccessRequestDto request,
            @AuthenticationPrincipal UserDetails user,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(investorAccessService.submit(resolveCaller(user, userEmail, userId), request)));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus(
            @AuthenticationPrincipal UserDetails user,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(investorAccessService.getStatus(resolveCaller(user, userEmail, userId))));
    }

    @GetMapping("/listings")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPublishedListings(
            @AuthenticationPrincipal UserDetails user,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(investorAccessService.getPublishedListings(resolveCaller(user, userEmail, userId))));
    }

    @PostMapping("/listings/{id}/interest")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendInvestorInterest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                investorAccessService.sendInvestorInterest(id, resolveCaller(user, userEmail, userId))
        ));
    }

    @DeleteMapping("/listings/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deletePublishedListing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                investorAccessService.deletePublishedListing(id, resolveCaller(user, userEmail, userId))
        ));
    }
}
