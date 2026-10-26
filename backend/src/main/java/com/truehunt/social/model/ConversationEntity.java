package com.truehunt.social.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Entity
@Table(name = "conversations")
public class ConversationEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String initials;
    private String color = "blue";
    private String preview;
    private String date;
    private String category = "Connections";
    private boolean unread = false;
    private boolean sponsored = false;
    private String subject;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    private String mailbox = "Focused";

    @Lob
    @Column(columnDefinition = "TEXT")
    private String threadJson = "[]";

    private LocalDateTime updatedAt = LocalDateTime.now();

    public ConversationEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isUnread() { return unread; }
    public void setUnread(boolean unread) { this.unread = unread; }

    public boolean isSponsored() { return sponsored; }
    public void setSponsored(boolean sponsored) { this.sponsored = sponsored; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMailbox() { return mailbox; }
    public void setMailbox(String mailbox) { this.mailbox = mailbox; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Map<String, Object>> getThread() {
        if (threadJson == null || threadJson.isBlank()) return new ArrayList<>();
        try {
            return MAPPER.readValue(threadJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setThread(List<Map<String, Object>> thread) {
        if (thread == null) {
            this.threadJson = "[]";
        } else {
            try {
                this.threadJson = MAPPER.writeValueAsString(thread);
            } catch (Exception e) {
                this.threadJson = "[]";
            }
        }
    }

    public void addMessage(String content, String direction, Map<String, Object> attachment) {
        List<Map<String, Object>> thread = getThread();
        Map<String, Object> msg = new HashMap<>();
        msg.put("id", "msg-" + System.currentTimeMillis() + "-" + thread.size());
        msg.put("content", content);
        msg.put("direction", direction);
        if (attachment != null) {
            msg.put("attachment", attachment);
        }
        msg.put("createdAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));
        thread.add(msg);
        setThread(thread);
        this.preview = content;
        this.date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd"));
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Object> toDto() {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", id);
        dto.put("name", name != null ? name : "");
        dto.put("initials", initials != null ? initials : "");
        dto.put("color", color != null ? color : "blue");
        dto.put("preview", preview != null ? preview : "");
        dto.put("date", date != null ? date : "");
        dto.put("category", category != null ? category : "Connections");
        dto.put("unread", unread);
        dto.put("sponsored", sponsored);
        dto.put("subject", subject != null ? subject : "");
        dto.put("description", description != null ? description : "");
        dto.put("mailbox", mailbox != null ? mailbox : "Focused");
        dto.put("thread", getThread());
        return dto;
    }
}
