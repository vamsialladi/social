package com.truehunt.social.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "profile_endorsements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_profile_endorsements_receiver_endorser",
                columnNames = {"receiver_profile_id", "endorser_profile_id"}
        )
)
public class EndorsementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiver_profile_id", nullable = false, length = 128)
    private String receiverProfileId;

    @Column(name = "endorser_profile_id", nullable = false, length = 128)
    private String endorserProfileId;

    @Lob
    @Column(name = "payload_json", columnDefinition = "TEXT", nullable = false)
    private String payloadJson;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReceiverProfileId() {
        return receiverProfileId;
    }

    public void setReceiverProfileId(String receiverProfileId) {
        this.receiverProfileId = receiverProfileId;
    }

    public String getEndorserProfileId() {
        return endorserProfileId;
    }

    public void setEndorserProfileId(String endorserProfileId) {
        this.endorserProfileId = endorserProfileId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}