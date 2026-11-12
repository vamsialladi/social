package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deletePost(@PathVariable Long id) {
        adminService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Post deleted successfully")));
    }
}
