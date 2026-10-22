package com.social.service;

import com.social.dto.request.AttachMediaRequest;
import com.social.dto.response.MediaUploadResponse;
import com.social.dto.response.ScopedUploadUrlResponse;
import com.social.entity.MediaFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {
    MediaUploadResponse uploadFile(MultipartFile file, String ownerId);
    Resource loadFileAsResource(String objectKey);
    MediaFile getMediaByObjectKey(String objectKey);
    MediaUploadResponse attachMediaToPost(String postId, AttachMediaRequest request, String ownerId);
    ScopedUploadUrlResponse generateScopedUploadUrl(String fileName, String contentType, long sizeBytes, String ownerId);
}
