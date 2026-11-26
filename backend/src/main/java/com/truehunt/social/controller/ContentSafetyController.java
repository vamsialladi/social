package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.ContentSafetyRequest;
import com.truehunt.social.dto.ContentSafetyResult;
import com.truehunt.social.service.ContentSafetyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/content-safety")
public class ContentSafetyController {

    private final ContentSafetyService contentSafetyService;

    public ContentSafetyController(ContentSafetyService contentSafetyService) {
        this.contentSafetyService = contentSafetyService;
    }

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<ContentSafetyResult>> scanContent(@RequestBody(required = false) ContentSafetyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(contentSafetyService.checkContent(request)));
    }
}
