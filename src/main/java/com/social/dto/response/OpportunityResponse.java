package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityResponse {

    @JsonProperty("_id")
    private String mongoId;

    private String id;
    private String authorId;
    private String authorName;
    private String authorAvatarUrl;
    private String title;
    private String content;
    private String type;
    private List<String> mediaUrls;
    private String employer;
    private String compensation;
    private String location;
    private Instant expiryDate;
    private Integer applicationsCount;
    private Instant createdAt;
    private Instant updatedAt;

    public String getMongoId() {
        return id;
    }
}
