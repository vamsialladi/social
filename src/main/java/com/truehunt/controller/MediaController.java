package com.truehunt.controller;

import com.truehunt.dto.response.ApiResponse;
import com.truehunt.model.MediaAttachment;
import com.truehunt.service.MediaStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @Autowired
    private MediaStorageService mediaStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MediaAttachment>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category) {

        MediaAttachment attachment = mediaStorageService.storeFile(file, category);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(attachment));
    }

    @GetMapping("/{fileKey}")
    public ResponseEntity<Resource> serveMedia(
            @PathVariable String fileKey,
            HttpServletRequest request) {

        Resource resource = mediaStorageService.loadFileAsResource(fileKey);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // fallback
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
