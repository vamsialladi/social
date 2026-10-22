package com.social.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class Attachment {
    private String id;
    private String url;
    private String type;
    private String name;
    private Long size;

    public Attachment() {
    }

    public Attachment(String id, String url, String type, String name, Long size) {
        this.id = id;
        this.url = url;
        this.type = type;
        this.name = name;
        this.size = size;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String url;
        private String type;
        private String name;
        private Long size;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder size(Long size) {
            this.size = size;
            return this;
        }

        public Attachment build() {
            return new Attachment(id, url, type, name, size);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
