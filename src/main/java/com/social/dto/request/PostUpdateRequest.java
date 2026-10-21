package com.social.dto.request;

import com.social.dto.response.MediaFileReferenceDto;
import com.social.entity.PostVisibility;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PostUpdateRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 10000, message = "Content must not exceed 10000 characters")
    private String content;

    private List<MediaFileReferenceDto> attachments;

    private PostVisibility visibility;

    public PostUpdateRequest() {
    }

    public PostUpdateRequest(String title, String content, List<MediaFileReferenceDto> attachments, PostVisibility visibility) {
        this.title = title;
        this.content = content;
        this.attachments = attachments;
        this.visibility = visibility;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String content;
        private List<MediaFileReferenceDto> attachments;
        private PostVisibility visibility;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder attachments(List<MediaFileReferenceDto> attachments) {
            this.attachments = attachments;
            return this;
        }

        public Builder visibility(PostVisibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public PostUpdateRequest build() {
            return new PostUpdateRequest(title, content, attachments, visibility);
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<MediaFileReferenceDto> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MediaFileReferenceDto> attachments) {
        this.attachments = attachments;
    }

    public PostVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(PostVisibility visibility) {
        this.visibility = visibility;
    }
}
