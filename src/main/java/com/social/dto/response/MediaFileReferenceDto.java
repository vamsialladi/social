package com.social.dto.response;

import com.social.entity.MediaFileReference;

public class MediaFileReferenceDto {

    private String objectKey;
    private String url;
    private String fileName;
    private String contentType;
    private Long sizeBytes;

    public MediaFileReferenceDto() {
    }

    public MediaFileReferenceDto(String objectKey, String url, String fileName, String contentType, Long sizeBytes) {
        this.objectKey = objectKey;
        this.url = url;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public static MediaFileReferenceDto fromEntity(MediaFileReference entity) {
        if (entity == null) {
            return null;
        }
        return new MediaFileReferenceDto(
                entity.getObjectKey(),
                entity.getUrl(),
                entity.getFileName(),
                entity.getContentType(),
                entity.getSizeBytes()
        );
    }

    public MediaFileReference toEntity() {
        return new MediaFileReference(
                this.objectKey,
                this.url,
                this.fileName,
                this.contentType,
                this.sizeBytes
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String objectKey;
        private String url;
        private String fileName;
        private String contentType;
        private Long sizeBytes;

        public Builder objectKey(String objectKey) {
            this.objectKey = objectKey;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder sizeBytes(Long sizeBytes) {
            this.sizeBytes = sizeBytes;
            return this;
        }

        public MediaFileReferenceDto build() {
            return new MediaFileReferenceDto(objectKey, url, fileName, contentType, sizeBytes);
        }
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
