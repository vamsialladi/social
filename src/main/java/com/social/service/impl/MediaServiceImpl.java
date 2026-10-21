package com.social.service.impl;

import com.social.dto.request.AttachMediaRequest;
import com.social.dto.response.MediaUploadResponse;
import com.social.dto.response.ScopedUploadUrlResponse;
import com.social.entity.MediaFile;
import com.social.exception.BadRequestException;
import com.social.exception.FileStorageException;
import com.social.exception.ResourceNotFoundException;
import com.social.repository.MediaFileRepository;
import com.social.service.MediaService;
import jakarta.annotation.PostConstruct;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MediaServiceImpl implements MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaServiceImpl.class);

    private final MediaFileRepository mediaFileRepository;
    private final Tika tika = new Tika();

    @Value("${app.media.storage-dir:uploads}")
    private String storageDir;

    @Value("${app.media.base-url:/api/media/files/}")
    private String baseUrl;

    @Value("${app.media.allowed-content-types:image/jpeg,image/png,image/webp,image/gif,video/mp4,video/webm,video/quicktime,audio/mpeg,audio/wav,audio/ogg,audio/mp4,audio/aac,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain}")
    private List<String> allowedContentTypes;

    private Path storageLocation;

    public MediaServiceImpl(MediaFileRepository mediaFileRepository) {
        this.mediaFileRepository = mediaFileRepository;
    }

    @PostConstruct
    public void init() {
        this.storageLocation = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
            log.info("Media storage directory initialized at: {}", this.storageLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize media storage directory: " + this.storageLocation, e);
        }
    }

    @Override
    @Transactional
    public MediaUploadResponse uploadFile(MultipartFile file, String ownerId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "file"));
        if (originalFilename.contains("..")) {
            throw new BadRequestException("File name contains invalid path sequence: " + originalFilename);
        }

        // Detect real content type from bytes via Apache Tika
        String detectedContentType;
        try (InputStream inputStream = file.getInputStream()) {
            detectedContentType = tika.detect(inputStream, originalFilename);
        } catch (IOException e) {
            throw new FileStorageException("Failed to inspect uploaded file contents", e);
        }

        if (detectedContentType == null || !isContentTypeAllowed(detectedContentType)) {
            throw new BadRequestException("Disallowed or unsupported file type: " + detectedContentType);
        }

        // Validate extension compatibility with detected MIME type
        validateExtensionCompatibility(originalFilename, detectedContentType);

        // Generate unique safe objectKey
        String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectKey = UUID.randomUUID().toString() + "_" + sanitizedFilename;

        Path targetLocation = this.storageLocation.resolve(objectKey).normalize();
        if (!targetLocation.startsWith(this.storageLocation)) {
            throw new BadRequestException("Invalid file destination path");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + originalFilename, e);
        }

        String fileUrl = baseUrl.endsWith("/") ? baseUrl + objectKey : baseUrl + "/" + objectKey;

        MediaFile mediaFile = MediaFile.builder()
                .objectKey(objectKey)
                .url(fileUrl)
                .fileName(originalFilename)
                .contentType(detectedContentType)
                .sizeBytes(file.getSize())
                .ownerId(ownerId)
                .build();

        MediaFile saved = mediaFileRepository.save(mediaFile);
        log.info("Uploaded media file: objectKey={}, ownerId={}, size={}", objectKey, ownerId, file.getSize());

        return MediaUploadResponse.fromEntity(saved);
    }

    @Override
    public Resource loadFileAsResource(String objectKey) {
        if (!StringUtils.hasText(objectKey) || objectKey.contains("..")) {
            throw new BadRequestException("Invalid object key");
        }

        Path filePath = this.storageLocation.resolve(objectKey).normalize();
        if (!filePath.startsWith(this.storageLocation)) {
            throw new BadRequestException("Access to target file path is forbidden");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Media file not found: " + objectKey);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Media file not found: " + objectKey);
        }
    }

    @Override
    public MediaFile getMediaByObjectKey(String objectKey) {
        return mediaFileRepository.findByObjectKey(objectKey)
                .orElseThrow(() -> new ResourceNotFoundException("Media record not found for key: " + objectKey));
    }

    @Override
    @Transactional
    public MediaUploadResponse attachMediaToPost(String postId, AttachMediaRequest request, String ownerId) {
        MediaFile mediaFile = mediaFileRepository.findByObjectKey(request.getObjectKey())
                .orElseGet(() -> MediaFile.builder()
                        .objectKey(request.getObjectKey())
                        .url(request.getUrl())
                        .fileName(request.getFileName())
                        .contentType(request.getContentType())
                        .sizeBytes(request.getSizeBytes())
                        .ownerId(ownerId)
                        .build());

        mediaFile.setPostId(postId);
        MediaFile saved = mediaFileRepository.save(mediaFile);
        return MediaUploadResponse.fromEntity(saved);
    }

    @Override
    public ScopedUploadUrlResponse generateScopedUploadUrl(String fileName, String contentType, long sizeBytes, String ownerId) {
        if (!isContentTypeAllowed(contentType)) {
            throw new BadRequestException("Disallowed content type for upload URL: " + contentType);
        }

        String sanitizedFilename = StringUtils.cleanPath(Objects.requireNonNullElse(fileName, "file"))
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectKey = UUID.randomUUID().toString() + "_" + sanitizedFilename;
        String uploadEndpoint = "/api/media/upload?objectKey=" + objectKey;

        return ScopedUploadUrlResponse.builder()
                .uploadUrl(uploadEndpoint)
                .objectKey(objectKey)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .maxSizeBytes(sizeBytes > 0 ? sizeBytes : 52428800L) // 50MB default
                .build();
    }

    private boolean isContentTypeAllowed(String contentType) {
        if (contentType == null) return false;
        String normalized = contentType.toLowerCase().split(";")[0].trim();
        return allowedContentTypes.stream().anyMatch(type -> type.equalsIgnoreCase(normalized));
    }

    private void validateExtensionCompatibility(String filename, String detectedContentType) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) return;
        String ext = filename.substring(dotIndex + 1).toLowerCase();

        Map<String, Set<String>> typeToExtensions = Map.of(
                "image/jpeg", Set.of("jpg", "jpeg"),
                "image/png", Set.of("png"),
                "image/webp", Set.of("webp"),
                "image/gif", Set.of("gif"),
                "video/mp4", Set.of("mp4", "m4v"),
                "video/webm", Set.of("webm"),
                "video/quicktime", Set.of("mov"),
                "audio/mpeg", Set.of("mp3"),
                "audio/wav", Set.of("wav"),
                "application/pdf", Set.of("pdf")
        );

        String baseType = detectedContentType.toLowerCase().split(";")[0].trim();
        Set<String> validExtensions = typeToExtensions.get(baseType);
        if (validExtensions != null && !validExtensions.contains(ext)) {
            log.warn("MIME type {} does not match file extension .{}", baseType, ext);
            if (!isExtensionCompatibleWithPrefix(baseType, ext)) {
                throw new BadRequestException("File extension ." + ext + " does not match detected content type " + baseType);
            }
        }
    }

    private boolean isExtensionCompatibleWithPrefix(String baseType, String ext) {
        if (baseType.startsWith("image/") && Set.of("jpg", "jpeg", "png", "webp", "gif").contains(ext)) return true;
        if (baseType.startsWith("video/") && Set.of("mp4", "mov", "webm", "avi", "mkv").contains(ext)) return true;
        if (baseType.startsWith("audio/") && Set.of("mp3", "wav", "ogg", "aac", "m4a").contains(ext)) return true;
        return false;
    }
}
