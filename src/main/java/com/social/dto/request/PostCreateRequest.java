package com.social.dto.request;

import com.social.dto.response.MediaFileReferenceDto;
import com.social.entity.PostType;
import com.social.entity.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class PostCreateRequest {

    private PostType type = PostType.POST;

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Content body must not be empty")
    @Size(max = 10000, message = "Content must not exceed 10000 characters")
    private String content;

    private List<MediaFileReferenceDto> attachments = new ArrayList<>();

    private List<String> mediaUrls = new ArrayList<>();

    private PostVisibility visibility = PostVisibility.PUBLIC;

    public PostCreateRequest() {
    }

    public PostCreateRequest(PostType type, String title, String content,
                             List<MediaFileReferenceDto> attachments, PostVisibility visibility) {
        this.type = type != null ? type : PostType.POST;
        this.title = title;
        this.content = content;
        this.attachments = attachments != null ? attachments : new ArrayList<>();
        this.visibility = visibility != null ? visibility : PostVisibility.PUBLIC;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PostType type = PostType.POST;
        private String title;
        private String content;
        private List<MediaFileReferenceDto> attachments = new ArrayList<>();
        private List<String> mediaUrls = new ArrayList<>();
        private PostVisibility visibility = PostVisibility.PUBLIC;

        public Builder type(PostType type) {
            this.type = type != null ? type : PostType.POST;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder attachments(List<MediaFileReferenceDto> attachments) {
            this.attachments = attachments != null ? attachments : new ArrayList<>();
            return this;
        }

        public Builder mediaUrls(List<String> mediaUrls) {
            this.mediaUrls = mediaUrls != null ? mediaUrls : new ArrayList<>();
            return this;
        }

        public Builder visibility(PostVisibility visibility) {
            this.visibility = visibility != null ? visibility : PostVisibility.PUBLIC;
            return this;
        }

        public PostCreateRequest build() {
            PostCreateRequest req = new PostCreateRequest(type, title, content, attachments, visibility);
            req.setMediaUrls(this.mediaUrls);
            return req;
        }
    }

    public PostType getType() {
        return type != null ? type : PostType.POST;
    }

    public void setType(PostType type) {
        this.type = type != null ? type : PostType.POST;
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
        this.attachments = attachments != null ? attachments : new ArrayList<>();
    }

    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls != null ? mediaUrls : new ArrayList<>();
    }

    public PostVisibility getVisibility() {
        return visibility != null ? visibility : PostVisibility.PUBLIC;
    }

    public void setVisibility(PostVisibility visibility) {
        this.visibility = visibility != null ? visibility : PostVisibility.PUBLIC;
    }
}
