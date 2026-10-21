package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    @JsonProperty("_id")
    private String mongoId;

    private String id;
    private String username;
    private String name;
    private String email;
    private String headline;
    private String avatarUrl;
    private String bio;
    private String role;
    private String location;
    private Instant createdAt;

    public String getMongoId() {
        return id;
    }
}
