package com.social.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    private List<String> mediaUrls = new ArrayList<>();

    public CreateOpportunityRequest() {
    }

    public CreateOpportunityRequest(String title, String content, String employer, String compensation,
                                    String location, Instant expiryDate, List<String> mediaUrls) {
        this.title = title;
        this.content = content;
        this.employer = employer;
        this.compensation = compensation;
        this.location = location;
        this.expiryDate = expiryDate;
        this.mediaUrls = mediaUrls != null ? mediaUrls : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String content;
        private String employer;
        private String compensation;
        private String location;
        private Instant expiryDate;
        private List<String> mediaUrls = new ArrayList<>();

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder employer(String employer) {
            this.employer = employer;
            return this;
        }

        public Builder compensation(String compensation) {
            this.compensation = compensation;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder expiryDate(Instant expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public Builder mediaUrls(List<String> mediaUrls) {
            this.mediaUrls = mediaUrls != null ? mediaUrls : new ArrayList<>();
            return this;
        }

        public CreateOpportunityRequest build() {
            return new CreateOpportunityRequest(title, content, employer, compensation, location, expiryDate, mediaUrls);
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getEmployer() {
        return employer;
    }

    public void setEmployer(String employer) {
        this.employer = employer;
    }

    public String getCompensation() {
        return compensation;
    }

    public void setCompensation(String compensation) {
        this.compensation = compensation;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
    }
}
