package com.truehunt.service;

import com.truehunt.exception.ValidationException;
import com.truehunt.model.MediaAttachment;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class MediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(MediaStorageService.class);

    @Value("${app.media.upload-dir:./uploads}")
    private String uploadDirPath;

    @Value("${app.media.max-demo-video-size-bytes:5242880}") // 5MB
    private long maxDemoVideoSizeBytes;

    @Value("${app.media.base-url:http://localhost:8080/api/media}")
    private String mediaBaseUrl;

    private Path uploadLocation;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "svg",
            "mp4", "mov", "avi", "webm", "mkv",
            "mp3", "wav", "ogg", "aac",
            "pdf", "doc", "docx", "txt"
    );

    @PostConstruct
    public void init() {
        try {
            this.uploadLocation = Paths.get(uploadDirPath).toAbsolutePath().normalize();
            Files.createDirectories(this.uploadLocation);
            log.info("Media storage directory initialized at: {}", this.uploadLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    public MediaAttachment storeFile(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Uploaded file cannot be empty.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String extension = "";
        int extIndex = originalFilename.lastIndexOf('.');
        if (extIndex > 0) {
            extension = originalFilename.substring(extIndex + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ValidationException("Unsupported file type: ." + extension);
        }

        // Server-side demo video limit enforcement (5 MB limit according to spec)
        if ("demo_video".equalsIgnoreCase(category) || isVideo(file.getContentType(), extension)) {
            if ("demo_video".equalsIgnoreCase(category) && file.getSize() > maxDemoVideoSizeBytes) {
                throw new ValidationException("Demo video file size exceeds the maximum limit of 5 MB.");
            }
        }

        String objectKey = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        try {
            Path targetPath = this.uploadLocation.resolve(objectKey);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String url = mediaBaseUrl + "/" + objectKey;
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            return new MediaAttachment(objectKey, url, originalFilename, contentType, file.getSize());
        } catch (IOException e) {
            log.error("Failed to store uploaded file: {}", originalFilename, e);
            throw new RuntimeException("Failed to store file on server", e);
        }
    }

    public Resource loadFileAsResource(String objectKey) {
        try {
            Path filePath = this.uploadLocation.resolve(objectKey).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new com.truehunt.exception.ResourceNotFoundException("File not found: " + objectKey);
            }
        } catch (MalformedURLException ex) {
            throw new com.truehunt.exception.ResourceNotFoundException("File path invalid: " + objectKey);
        }
    }

    public String buildMediaUrl(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return null;
        }
        if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
            return objectKey;
        }
        return mediaBaseUrl + "/" + objectKey;
    }

    private boolean isVideo(String contentType, String extension) {
        if (contentType != null && contentType.startsWith("video/")) {
            return true;
        }
        return Arrays.asList("mp4", "mov", "avi", "webm", "mkv").contains(extension);
    }
}
