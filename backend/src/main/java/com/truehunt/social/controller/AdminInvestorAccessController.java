package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.service.InvestorAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/investor-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInvestorAccessController {

    private final InvestorAccessService investorAccessService;

    public AdminInvestorAccessController(InvestorAccessService investorAccessService) {
        this.investorAccessService = investorAccessService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllRequests() {
        return ResponseEntity.ok(ApiResponse.success(investorAccessService.getAllRequests()));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails admin
    ) {
        return ResponseEntity.ok(ApiResponse.success(investorAccessService.review(id, "APPROVED", admin.getUsername())));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails admin
    ) {
        return ResponseEntity.ok(ApiResponse.success(investorAccessService.review(id, "REJECTED", admin.getUsername())));
    }
}
