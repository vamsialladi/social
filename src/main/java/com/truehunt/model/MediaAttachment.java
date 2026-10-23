package com.truehunt.model;

import java.io.Serializable;

public class MediaAttachment implements Serializable {
    private String objectKey;
    private String url;
    private String fileName;
    private String contentType;
    private Long sizeBytes;

    public MediaAttachment() {}

    public MediaAttachment(String objectKey, String url, String fileName, String contentType, Long sizeBytes) {
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
