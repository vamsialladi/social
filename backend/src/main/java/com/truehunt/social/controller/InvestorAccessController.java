package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.InvestorAccessRequestDto;
import com.truehunt.social.service.InvestorAccessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/investor-access")
@PreAuthorize("isAuthenticated()")
public class InvestorAccessController {

    private final InvestorAccessService investorAccessService;

    public InvestorAccessController(InvestorAccessService investorAccessService) {
        this.investorAccessService = investorAccessService;
    }

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submit(
            @Valid @RequestBody InvestorAccessRequestDto request,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(ApiResponse.success(investorAccessService.submit(user.getUsername(), request)));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(investorAccessService.getStatus(user.getUsername())));
    }

    @GetMapping("/listings")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPublishedListings(
            @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(ApiResponse.success(investorAccessService.getPublishedListings(user.getUsername())));
    }

    @DeleteMapping("/listings/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deletePublishedListing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                investorAccessService.deletePublishedListing(id, user.getUsername())
        ));
    }
}
