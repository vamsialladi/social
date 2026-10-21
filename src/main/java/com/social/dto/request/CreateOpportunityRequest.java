package com.social.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class CreateOpportunityRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Content / description is required")
    @Size(max = 10000, message = "Content must not exceed 10000 characters")
    private String content;

    @Size(max = 150, message = "Employer name must not exceed 150 characters")
    private String employer;

    @Size(max = 100, message = "Compensation must not exceed 100 characters")
    private String compensation;

    @Size(max = 150, message = "Location must not exceed 150 characters")
    private String location;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant expiryDate;

    private List<String> mediaUrls;
}
