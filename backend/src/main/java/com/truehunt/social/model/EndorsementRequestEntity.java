package com.truehunt.social.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "endorsement_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_endorsement_requests_sender_recipient",
                columnNames = {"sender_profile_id", "recipient_profile_id"}
        )
)
public class EndorsementRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_profile_id", nullable = false, length = 128)
    private String senderProfileId;

    @Column(name = "recipient_profile_id", nullable = false, length = 128)
    private String recipientProfileId;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    public EndorsementRequestEntity() {}

    public EndorsementRequestEntity(String senderProfileId, String recipientProfileId, LocalDateTime requestedAt) {
        this.senderProfileId = senderProfileId;
        this.recipientProfileId = recipientProfileId;
        this.requestedAt = requestedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSenderProfileId() { return senderProfileId; }
    public void setSenderProfileId(String senderProfileId) { this.senderProfileId = senderProfileId; }

    public String getRecipientProfileId() { return recipientProfileId; }
    public void setRecipientProfileId(String recipientProfileId) { this.recipientProfileId = recipientProfileId; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
}
