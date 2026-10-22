package com.social.dto.request;

import jakarta.validation.constraints.NotBlank;

public class SendConnectionRequest {

    @NotBlank(message = "recipientId is required")
    private String recipientId;

    public SendConnectionRequest() {
    }

    public SendConnectionRequest(String recipientId) {
        this.recipientId = recipientId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public static class Builder {
        private String recipientId;

        public Builder recipientId(String recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        public SendConnectionRequest build() {
            return new SendConnectionRequest(recipientId);
        }
    }
}
