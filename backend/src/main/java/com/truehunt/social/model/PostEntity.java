package com.truehunt.social.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Entity
@Table(name = "posts")
public class PostEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String authorId;
    private String authorName;

    @Column(length = 2048)
    private String authorAvatar;

    private String authorHeadline;

    @Column(nullable = false)
    private String type = "post"; // "post", "article", "opportunity"

    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    private String mediaUrl;
    private String mediaType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String mediaJson;

    private int likesCount = 0;
    private int commentsCount = 0;
    private int applicationsCount = 0;
    private boolean likedByMe = false;
    private boolean appliedByMe = false;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String commentsJson = "[]";

    private LocalDateTime createdAt = LocalDateTime.now();

    public PostEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }

    public String getAuthorHeadline() { return authorHeadline; }
    public void setAuthorHeadline(String authorHeadline) { this.authorHeadline = authorHeadline; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }

    public int getApplicationsCount() { return applicationsCount; }
    public void setApplicationsCount(int applicationsCount) { this.applicationsCount = applicationsCount; }

    public boolean isLikedByMe() { return likedByMe; }
    public void setLikedByMe(boolean likedByMe) { this.likedByMe = likedByMe; }

    public boolean isLiked() { return likedByMe; }
    public void setLiked(boolean liked) { this.likedByMe = liked; }

    public boolean isAppliedByMe() { return appliedByMe; }
    public void setAppliedByMe(boolean appliedByMe) { this.appliedByMe = appliedByMe; }

    public boolean isApplied() { return appliedByMe; }
    public void setApplied(boolean applied) { this.appliedByMe = applied; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> getMedia() {
        if (mediaJson != null && !mediaJson.isBlank()) {
            try {
                return MAPPER.readValue(mediaJson, new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {}
        }
        if (mediaUrl != null && !mediaUrl.isBlank()) {
            Map<String, Object> m = new HashMap<>();
            m.put("url", mediaUrl);
            m.put("type", mediaType != null ? mediaType : "image");
            return m;
        }
        return null;
    }

    public void setMedia(Map<String, Object> media) {
        if (media == null) {
            this.mediaJson = null;
        } else {
            try {
                this.mediaJson = MAPPER.writeValueAsString(media);
            } catch (Exception e) {
                this.mediaJson = null;
            }
        }
    }

    public void setOpportunityDetails(Map<String, Object> details) {
        setMedia(details);
    }

    public List<Map<String, Object>> getComments() {
        if (commentsJson == null || commentsJson.isBlank()) return new ArrayList<>();
        try {
            return MAPPER.readValue(commentsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setComments(List<Map<String, Object>> comments) {
        if (comments == null) {
            this.commentsJson = "[]";
            this.commentsCount = 0;
        } else {
            try {
                this.commentsJson = MAPPER.writeValueAsString(comments);
                this.commentsCount = comments.size();
            } catch (Exception e) {
                this.commentsJson = "[]";
                this.commentsCount = 0;
            }
        }
    }

    public void toggleLike() {
        this.likedByMe = !this.likedByMe;
        if (this.likedByMe) {
            this.likesCount++;
        } else {
            this.likesCount = Math.max(0, this.likesCount - 1);
        }
    }

    public void addComment(String author, String text) {
        List<Map<String, Object>> list = getComments();
        Map<String, Object> c = new HashMap<>();
        c.put("id", "c-" + System.currentTimeMillis());
        c.put("authorName", author);
        c.put("text", text);
        c.put("createdAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));
        list.add(c);
        setComments(list);
    }

    public void toggleApply() {
        this.appliedByMe = !this.appliedByMe;
        if (this.appliedByMe) {
            this.applicationsCount++;
        } else {
            this.applicationsCount = Math.max(0, this.applicationsCount - 1);
        }
    }

    public Map<String, Object> toDto() {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", String.valueOf(id));
        dto.put("authorId", authorId != null ? authorId : "");
        dto.put("authorName", authorName != null ? authorName : "");
        dto.put("authorAvatar", authorAvatar != null ? authorAvatar : "");
        dto.put("authorHeadline", authorHeadline != null ? authorHeadline : "");
        dto.put("type", type != null ? type : "post");
        dto.put("title", title != null ? title : "");
        dto.put("content", content != null ? content : "");
        dto.put("media", getMedia());
        dto.put("mediaUrl", mediaUrl);
        dto.put("mediaType", mediaType);
        dto.put("likesCount", likesCount);
        dto.put("commentsCount", commentsCount);
        dto.put("applicationsCount", applicationsCount);
        dto.put("liked", likedByMe);
        dto.put("applied", appliedByMe);
        dto.put("comments", getComments());
        dto.put("createdAt", createdAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a")));
        return dto;
    }
}
