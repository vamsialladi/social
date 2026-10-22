package com.social.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conv_last_msg_at", columnList = "last_message_at")
})
public class Conversation {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    @JsonProperty("id")
    private String id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "conversation_participants",
            joinColumns = @JoinColumn(name = "conversation_id"),
            indexes = @Index(name = "idx_participant_user_id", columnList = "participant_id")
    )
    @Column(name = "participant_id", nullable = false)
    private List<String> participantIds = new ArrayList<>();

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Conversation() {
    }

    public Conversation(String id, List<String> participantIds, Instant lastMessageAt, Instant createdAt) {
        this.id = id;
        this.participantIds = participantIds != null ? participantIds : new ArrayList<>();
        this.lastMessageAt = lastMessageAt;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (lastMessageAt == null) {
            lastMessageAt = createdAt;
        }
    }

    @JsonProperty("_id")
    public String get_id() {
        return id;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private List<String> participantIds = new ArrayList<>();
        private Instant lastMessageAt;
        private Instant createdAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder participantIds(List<String> participantIds) {
            this.participantIds = participantIds;
            return this;
        }

        public Builder lastMessageAt(Instant lastMessageAt) {
            this.lastMessageAt = lastMessageAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Conversation build() {
            return new Conversation(id, participantIds, lastMessageAt, createdAt);
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
