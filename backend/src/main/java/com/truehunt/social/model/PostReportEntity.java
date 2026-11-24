package com.truehunt.social.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "post_reports")
public class PostReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_email", nullable = false, length = 255)
    private String reporterEmail;

    @Column(name = "post_id", nullable = false, length = 64)
    private String postId;

    @Column(name = "author_id", length = 255)
    private String authorId;

    @Column(name = "author_name", length = 255)
    private String authorName;

    @Column(length = 500)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false, length = 32)
    private String status = "OPEN"; // OPEN, REVIEWED, RESOLVED, DISMISSED

    private LocalDateTime reportedAt = LocalDateTime.now();

    public PostReportEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }

    public Map<String, Object> toDto() {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", id);
        dto.put("reporterEmail", reporterEmail != null ? reporterEmail : "");
        dto.put("postId", postId != null ? postId : "");
        dto.put("authorId", authorId != null ? authorId : "");
        dto.put("authorName", authorName != null ? authorName : "");
        dto.put("title", title != null ? title : "");
        dto.put("content", content != null ? content : "");
        dto.put("reason", reason != null ? reason : "");
        dto.put("status", status != null ? status : "OPEN");
        dto.put("reportedAt", reportedAt != null ? reportedAt.toString() : "");
        return dto;
    }
}
