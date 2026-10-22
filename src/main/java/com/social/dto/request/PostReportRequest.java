package com.social.dto.request;

import jakarta.validation.constraints.Size;

public class PostReportRequest {

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    public PostReportRequest() {
    }

    public PostReportRequest(String reason) {
        this.reason = reason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String reason;

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public PostReportRequest build() {
            return new PostReportRequest(reason);
        }
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
