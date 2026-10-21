package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.social.entity.ConnectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionResponse {

    @JsonProperty("_id")
    private String mongoId;

    private String id;
    private String requesterId;
    private String recipientId;
    private ConnectionStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    // Optional enriched user details for frontend convenience
    private UserProfileResponse requester;
    private UserProfileResponse recipient;
    private UserProfileResponse connectedUser;

    public String getMongoId() {
        return id;
    }
}
