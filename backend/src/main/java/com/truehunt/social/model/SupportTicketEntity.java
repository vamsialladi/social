package com.truehunt.social.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
public class SupportTicketEntity {

    @Id
    private String id;

    private String name;
    private String email;
    private String subject;
    private String category;
    private String priority;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String message;

    private String status = "OPEN";
    private LocalDateTime createdAt = LocalDateTime.now();

    public SupportTicketEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
