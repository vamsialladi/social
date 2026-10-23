package com.truehunt.model;

import com.truehunt.model.converter.StringListConverter;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conv_last_msg", columnList = "last_message_at")
})
public class Conversation {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Convert(converter = StringListConverter.class)
    @Column(name = "participant_ids", columnDefinition = "TEXT", nullable = false)
    private List<String> participantIds = new ArrayList<>();

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Conversation() {}

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        this.createdAt = now;
        if (this.lastMessageAt == null) {
            this.lastMessageAt = now;
        }
        if (this.participantIds == null) {
            this.participantIds = new ArrayList<>();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
