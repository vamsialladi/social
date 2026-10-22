package com.social.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreatePostRequest {

    @NotBlank(message = "Content is required")
    @Size(max = 10000, message = "Content must not exceed 10000 characters")
    private String content;

    private List<String> mediaUrls;

    public CreatePostRequest() {
    }

    public CreatePostRequest(String content, List<String> mediaUrls) {
        this.content = content;
        this.mediaUrls = mediaUrls;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    public static class Builder {
        private String content;
        private List<String> mediaUrls;

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder mediaUrls(List<String> mediaUrls) {
            this.mediaUrls = mediaUrls;
            return this;
        }

        public CreatePostRequest build() {
            return new CreatePostRequest(content, mediaUrls);
        }
    }
}
