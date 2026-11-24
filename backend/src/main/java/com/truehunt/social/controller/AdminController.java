package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.service.AdminService;
import com.truehunt.social.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final PostService postService;

    public AdminController(AdminService adminService) {
        this(adminService, null);
    }

    @Autowired
    public AdminController(AdminService adminService, PostService postService) {
        this.adminService = adminService;
        this.postService = postService;
    }

    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAccounts() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAccounts()));
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails admin
    ) {
        adminService.deleteAccount(id, admin.getUsername());
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Account deleted successfully")));
    }

    @PatchMapping("/accounts/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setAccountStatus(
            @PathVariable Long id,
            @RequestParam boolean active,
            @AuthenticationPrincipal UserDetails admin
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.setAccountActive(id, active, admin.getUsername())
        ));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deletePost(@PathVariable Long id) {
        adminService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Post deleted successfully")));
    }

    @DeleteMapping("/opportunities/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteOpportunity(@PathVariable Long id) {
        adminService.deleteOpportunity(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Opportunity deleted successfully")));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getReports() {
        return ResponseEntity.ok(ApiResponse.success(postService != null ? postService.getAllReports() : List.of()));
    }

    @PatchMapping("/reports/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateReportStatus(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String status = body != null ? body.getOrDefault("status", "REVIEWED") : "REVIEWED";
        return ResponseEntity.ok(ApiResponse.success(postService.updateReportStatus(id, status)));
    }
}
