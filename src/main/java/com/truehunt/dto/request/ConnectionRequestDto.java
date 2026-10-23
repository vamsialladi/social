package com.truehunt.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ConnectionRequestDto {

    @NotBlank(message = "Recipient ID is required.")
    private String recipientId;

    public ConnectionRequestDto() {}

    public ConnectionRequestDto(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }
}
