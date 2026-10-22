package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.social.entity.Post;
import com.social.entity.PostType;
import com.social.entity.PostVisibility;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostResponse {

    private String id;
    private String authorId;
    private String authorName;
    private String authorAvatarUrl;
    private String type;
    private String title;
    private String content;
    private List<MediaFileReferenceDto> attachments = new ArrayList<>();
    private List<String> mediaUrls = new ArrayList<>();
    private PostVisibility visibility;
    private Instant createdAt;
    private Instant updatedAt;

    public PostResponse() {
    }

    public PostResponse(String id, String authorId, String authorName, String authorAvatarUrl,
                        String type, String title, String content,
                        List<MediaFileReferenceDto> attachments, List<String> mediaUrls,
                        PostVisibility visibility, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.type = type;
        this.title = title;
        this.content = content;
        this.attachments = attachments != null ? attachments : new ArrayList<>();
        this.mediaUrls = mediaUrls != null ? mediaUrls : new ArrayList<>();
        this.visibility = visibility;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @JsonProperty("_id")
    public String getMongoId() {
        return id;
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
                .authorName(post.getAuthorName())
                .authorAvatarUrl(post.getAuthorAvatarUrl())
                .type(post.getType())
                .title(post.getTitle())
                .content(post.getContent())
                .attachments(attachmentDtos)
                .mediaUrls(post.getMediaUrls() != null ? post.getMediaUrls() : Collections.emptyList())
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
        private String authorName;
        private String authorAvatarUrl;
        private String type = "post";
        private String title;
        private String content;
        private List<MediaFileReferenceDto> attachments = new ArrayList<>();
        private List<String> mediaUrls = new ArrayList<>();
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

        public Builder authorName(String authorName) {
            this.authorName = authorName;
            return this;
        }

        public Builder authorAvatarUrl(String authorAvatarUrl) {
            this.authorAvatarUrl = authorAvatarUrl;
            return this;
        }

        public Builder type(String type) {
            this.type = type != null ? type.toLowerCase() : "post";
            return this;
        }

        public Builder type(PostType type) {
            this.type = type != null ? type.toValue() : "post";
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
            return new PostResponse(id, authorId, authorName, authorAvatarUrl, type, title, content,
                    attachments, mediaUrls, visibility, createdAt, updatedAt);
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

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }

    public PostType getType() {
        return type != null ? PostType.fromString(type) : null;
    }

    public void setType(PostType type) {
        this.type = type != null ? type.toValue() : null;
    }

    public void setType(String type) {
        this.type = type != null ? type.toLowerCase() : null;
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

    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
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
