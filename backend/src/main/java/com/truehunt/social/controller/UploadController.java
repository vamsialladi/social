package com.truehunt.social.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping({"/uploads", "/api/uploads"})
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private static final String DEFAULT_AVATAR_SVG =
            """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200" fill="none">
              <rect width="200" height="200" fill="#0f172a"/>
              <circle cx="100" cy="75" r="35" fill="#f59e0b"/>
              <path d="M40 170c0-33.137 26.863-60 60-60s60 26.863 60 60" fill="#f59e0b"/>
            </svg>
            """;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveUpload(@PathVariable String filename) {
        Path[] searchPaths = new Path[]{
                Paths.get(uploadDir, filename),
                Paths.get("backend", "uploads", filename),
                Paths.get("uploads", filename),
                Paths.get("..", "uploads", filename)
        };

        for (Path p : searchPaths) {
            try {
                if (Files.exists(p) && Files.isReadable(p)) {
                    Resource resource = new UrlResource(p.toUri());
                    String contentType = Files.probeContentType(p);
                    if (contentType == null) {
                        String lower = filename.toLowerCase();
                        if (lower.endsWith(".png")) contentType = "image/png";
                        else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) contentType = "image/jpeg";
                        else if (lower.endsWith(".webp")) contentType = "image/webp";
                        else if (lower.endsWith(".gif")) contentType = "image/gif";
                        else if (lower.endsWith(".svg")) contentType = "image/svg+xml";
                        else contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                    }
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                            .body(resource);
                }
            } catch (Exception e) {
                log.warn("Could not read file {}: {}", p, e.getMessage());
            }
        }

        // Return fallback avatar placeholder SVG with HTTP 200 so missing sample image URLs do not break UI or fail in devtools
        byte[] svgBytes = DEFAULT_AVATAR_SVG.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(new ByteArrayResource(svgBytes));
    }
}

