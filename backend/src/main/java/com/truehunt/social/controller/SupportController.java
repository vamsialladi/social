package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.SupportTicketRequest;
import com.truehunt.social.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping("/support")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitSupportTicket(@Valid @RequestBody SupportTicketRequest ticket) {
        return ResponseEntity.ok(ApiResponse.success(supportService.submitTicket(ticket)));
    }

    @GetMapping("/faqs")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFaqs() {
        return ResponseEntity.ok(ApiResponse.success(supportService.getFaqs()));
    }
}
