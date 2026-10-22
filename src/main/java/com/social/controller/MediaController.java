package com.social.controller;

import com.social.dto.request.AttachMediaRequest;
import com.social.dto.response.ApiResponse;
import com.social.dto.response.MediaUploadResponse;
import com.social.dto.response.ScopedUploadUrlResponse;
import com.social.entity.MediaFile;
import com.social.security.CurrentUser;
import com.social.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Tag(name = "Media Upload", description = "Endpoints for uploading, managing, and serving media assets")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @Operation(summary = "Upload a media file (multipart)", description = "Uploads an image, video, audio, or document, validates MIME type and stores file.")
    @PostMapping(value = "/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MediaUploadResponse>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @CurrentUser String ownerId) {
        MediaUploadResponse response = mediaService.uploadFile(file, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @Operation(summary = "Request a short-lived scoped upload URL", description = "Generates a pre-scoped upload destination for direct uploads.")
    @PostMapping("/media/upload/request-url")
    public ResponseEntity<ApiResponse<ScopedUploadUrlResponse>> requestUploadUrl(
            @RequestParam("fileName") String fileName,
            @RequestParam("contentType") String contentType,
            @RequestParam(value = "sizeBytes", defaultValue = "0") long sizeBytes,
            @CurrentUser String ownerId) {
        ScopedUploadUrlResponse response = mediaService.generateScopedUploadUrl(fileName, contentType, sizeBytes, ownerId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Attach uploaded media to a post", description = "Attaches an uploaded media object reference to an existing post.")
    @PostMapping("/posts/{postId}/media")
    public ResponseEntity<ApiResponse<MediaUploadResponse>> attachMediaToPost(
            @PathVariable("postId") String postId,
            @Valid @RequestBody AttachMediaRequest request,
            @CurrentUser String ownerId) {
        MediaUploadResponse response = mediaService.attachMediaToPost(postId, request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @Operation(summary = "Retrieve/Stream stored media file", description = "Serves the file by objectKey with appropriate content type and caching.")
    @GetMapping("/media/files/{objectKey}")
    public ResponseEntity<Resource> getMediaFile(@PathVariable("objectKey") String objectKey) {
        Resource file = mediaService.loadFileAsResource(objectKey);
        MediaFile metadata = mediaService.getMediaByObjectKey(objectKey);

        String contentType = (metadata != null && metadata.getContentType() != null)
                ? metadata.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + 
                        (metadata != null ? metadata.getFileName() : objectKey) + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(file);
    }
}
