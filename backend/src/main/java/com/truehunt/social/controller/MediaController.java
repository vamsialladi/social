package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("EMPTY_FILE", "Uploaded file is empty"));
        }

        // Validate max 5MB for demo videos as requested in BACKEND_FLOW.md
        if ("demo-video".equalsIgnoreCase(category) && file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(ApiResponse.error("FILE_TOO_LARGE", "Demo video must not exceed 5 MB"));
        }

        try {
            Path targetDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(targetDirectory)) {
                Files.createDirectories(targetDirectory);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String uniqueName = UUID.randomUUID().toString() + extension;
            Path targetFile = targetDirectory.resolve(uniqueName);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            String kind = "document";
            if (contentType.startsWith("image/")) kind = "image";
            else if (contentType.startsWith("video/")) kind = "video";
            else if (contentType.startsWith("audio/")) kind = "audio";

            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            String fileUrl = baseUrl + "/uploads/" + uniqueName;

            Map<String, Object> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("fileName", originalFilename);
            result.put("storedName", uniqueName);
            result.put("contentType", contentType);
            result.put("sizeBytes", file.getSize());
            result.put("kind", kind);

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("UPLOAD_FAILED", "Failed to store media file: " + e.getMessage()));
        }
    }
}
