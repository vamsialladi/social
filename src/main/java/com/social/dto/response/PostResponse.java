package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.social.entity.Post;
import com.social.entity.PostType;
import com.social.entity.PostVisibility;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostResponse {

    private String id;
    private String authorId;
    private PostType type;
    private String title;
    private String content;
    private List<MediaFileReferenceDto> attachments;
    private PostVisibility visibility;
    private Instant createdAt;
    private Instant updatedAt;

    public PostResponse() {
    }

    public PostResponse(String id, String authorId, PostType type, String title, String content,
                        List<MediaFileReferenceDto> attachments, PostVisibility visibility,
                        Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.authorId = authorId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.attachments = attachments;
        this.visibility = visibility;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PostResponse fromEntity(Post post) {
        if (post == null) {
            return null;
        }

        List<MediaFileReferenceDto> attachmentDtos = new ArrayList<>();
        if (post.getAttachments() != null) {
            attachmentDtos = post.getAttachments().stream()
                    .map(MediaFileReferenceDto::fromEntity)
                    .toList();
        }

        return PostResponse.builder()
                .id(post.getId())
                .authorId(post.getAuthorId())
                .type(post.getType())
                .title(post.getTitle())
                .content(post.getContent())
                .attachments(attachmentDtos)
                .visibility(post.getVisibility())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String authorId;
        private PostType type;
        private String title;
        private String content;
        private List<MediaFileReferenceDto> attachments;
        private PostVisibility visibility;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder authorId(String authorId) {
            this.authorId = authorId;
            return this;
        }

        public Builder type(PostType type) {
            this.type = type;
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
            this.attachments = attachments;
            return this;
        }

        public Builder visibility(PostVisibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public PostResponse build() {
            return new PostResponse(id, authorId, type, title, content, attachments, visibility, createdAt, updatedAt);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public PostType getType() {
        return type;
    }

    public void setType(PostType type) {
        this.type = type;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
