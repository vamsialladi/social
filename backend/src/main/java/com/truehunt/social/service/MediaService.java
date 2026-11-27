package com.truehunt.social.service;

import com.truehunt.social.dto.ContentSafetyResult;
import com.truehunt.social.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaService {

    private static final long MAX_PROFILE_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_GROUP_PHOTO_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> PROFILE_IMAGE_CONTENT_TYPES = Set.of(
            "image/avif",
            "image/gif",
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Set<String> GROUP_PHOTO_CONTENT_TYPES = Set.of(
            "image/avif",
            "image/bmp",
            "image/gif",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private final ContentSafetyService contentSafetyService;

    public MediaService(ContentSafetyService contentSafetyService) {
        this.contentSafetyService = contentSafetyService;
    }

    public Map<String, Object> uploadFile(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty or missing");
        }

        if ("profile-image".equalsIgnoreCase(category)) {
            validateImageFile(file);
            return encodeProfileImage(file, category);
        }
        if ("connection-group-photo".equalsIgnoreCase(category)) {
            validateImageFile(file, MAX_GROUP_PHOTO_SIZE_BYTES, "10 MB", GROUP_PHOTO_CONTENT_TYPES);
        } else if (isImageFile(file)) {
            // Page-post images are checked by the existing client-side NSFWJS upload flow.
            if (!"connection-group-post".equalsIgnoreCase(category)) {
                moderateImage(file);
            }
        }

        try {
            Path targetDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "media";
            String ext = "";
            int dotIdx = originalName.lastIndexOf('.');
            if (dotIdx > 0) {
                ext = originalName.substring(dotIdx);
            }

            String storedFileName = UUID.randomUUID().toString() + ext;
            Path targetPath = targetDir.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/" + storedFileName;

            return buildUploadResult(fileUrl, originalName, file.getSize(), file.getContentType(),
                    category != null ? category : "general");
        } catch (IOException e) {
            throw new RuntimeException("Failed to store media file: " + e.getMessage(), e);
        }
    }

    private void moderateImage(MultipartFile file) {
        try {
            ContentSafetyResult result = contentSafetyService.checkImage(file.getBytes());
            if (!result.isSafe()) {
                throw new BadRequestException("MEDIA_CONTENT_REJECTED", result.getReason());
            }
        } catch (IOException exception) {
            throw new BadRequestException(
                    "MEDIA_SCAN_FAILED",
                    "The image could not be checked for adult content. Please try again."
            );
        }
    }

    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        String fileName = file.getOriginalFilename();
        return fileName != null
                && fileName.toLowerCase(Locale.ROOT).matches(".*\\.(avif|bmp|gif|jpe?g|png|svg|webp)$");
    }

    private Map<String, Object> encodeProfileImage(MultipartFile file, String category) {
        validateImageFile(file);
        try {
            String contentType = file.getContentType().toLowerCase();
            String dataUrl = "data:" + contentType + ";base64,"
                    + Base64.getEncoder().encodeToString(file.getBytes());
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "profile-image";

            return buildUploadResult(dataUrl, originalName, file.getSize(), contentType, category);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process profile image: " + e.getMessage(), e);
        }
    }

    private void validateImageFile(MultipartFile file) {
        validateImageFile(file, MAX_PROFILE_IMAGE_SIZE_BYTES, "5 MB", PROFILE_IMAGE_CONTENT_TYPES);
    }

    private void validateImageFile(
            MultipartFile file,
            long maxSizeBytes,
            String maxSizeLabel,
            Set<String> supportedContentTypes) {
        String contentType = file.getContentType();
        if (contentType == null || !supportedContentTypes.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Image must be a supported image format");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new BadRequestException("Image must be no larger than " + maxSizeLabel);
        }
    }

    private Map<String, Object> buildUploadResult(
            String url,
            String fileName,
            long size,
            String contentType,
            String category
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("url", url);
        result.put("fileName", fileName);
        result.put("size", size);
        result.put("sizeBytes", size);
        result.put("contentType", contentType);
        result.put("category", category != null ? category : "general");
        result.put("kind", resolveKind(contentType, fileName));
        return result;
    }

    private String resolveKind(String contentType, String fileName) {
        if (contentType != null && !contentType.isBlank()) {
            String lower = contentType.toLowerCase();
            if (lower.startsWith("image/")) {
                return "image";
            }
            if (lower.startsWith("video/")) {
                return "video";
            }
            if (lower.startsWith("audio/")) {
                return "audio";
            }
        }
        if (fileName != null && !fileName.isBlank()) {
            String lowerName = fileName.toLowerCase();
            if (lowerName.matches(".*\\.(mp4|webm|mov|m4v|avi|mkv|ogg|ogv|3gp|flv|wmv|mpeg|mpg)$")) {
                return "video";
            }
            if (lowerName.matches(".*\\.(jpg|jpeg|png|gif|webp|avif|bmp|svg)$")) {
                return "image";
            }
            if (lowerName.matches(".*\\.(mp3|wav|aac|flac|m4a)$")) {
                return "audio";
            }
        }
        return "file";
    }
}
