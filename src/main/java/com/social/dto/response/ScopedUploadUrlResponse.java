package com.social.dto.response;

import java.time.Instant;

public class ScopedUploadUrlResponse {
    private String uploadUrl;
    private String objectKey;
    private Instant expiresAt;
    private long maxSizeBytes;

    public ScopedUploadUrlResponse() {
    }

    public ScopedUploadUrlResponse(String uploadUrl, String objectKey, Instant expiresAt, long maxSizeBytes) {
        this.uploadUrl = uploadUrl;
        this.objectKey = objectKey;
        this.expiresAt = expiresAt;
        this.maxSizeBytes = maxSizeBytes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String uploadUrl;
        private String objectKey;
        private Instant expiresAt;
        private long maxSizeBytes;

        public Builder uploadUrl(String uploadUrl) {
            this.uploadUrl = uploadUrl;
            return this;
        }

        public Builder objectKey(String objectKey) {
            this.objectKey = objectKey;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder maxSizeBytes(long maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
            return this;
        }

        public ScopedUploadUrlResponse build() {
            return new ScopedUploadUrlResponse(uploadUrl, objectKey, expiresAt, maxSizeBytes);
        }
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }
}
