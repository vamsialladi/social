package com.social.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AttachMediaRequest {

    @NotBlank(message = "objectKey is required")
    private String objectKey;

    @NotBlank(message = "url is required")
    private String url;

    @NotBlank(message = "fileName is required")
    private String fileName;

    @NotBlank(message = "contentType is required")
    private String contentType;

    @NotNull(message = "sizeBytes is required")
    @Positive(message = "sizeBytes must be greater than zero")
    private Long sizeBytes;

    public AttachMediaRequest() {
    }

    public AttachMediaRequest(String objectKey, String url, String fileName, String contentType, Long sizeBytes) {
        this.objectKey = objectKey;
        this.url = url;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
}
