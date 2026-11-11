package com.truehunt.social.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recipientId;
    private String title;
    private String message;
    private String type = "endorsement";
    private boolean isRead = false;
    private LocalDateTime createdAt = LocalDateTime.now();

    public NotificationEntity() {}

    public NotificationEntity(String recipientId, String title, String message, String type) {
        this.recipientId = recipientId;
        this.title = title;
        this.message = message;
        this.type = type;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> toDto() {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", id);
        dto.put("recipientId", recipientId);
        dto.put("title", title);
        dto.put("message", message);
        dto.put("type", type);
        dto.put("read", isRead);
        dto.put("createdAt", createdAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        return dto;
    }
}
