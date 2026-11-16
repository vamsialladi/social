package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateConversationRequest {
    @NotBlank(message = "Recipient is required")
    @JsonAlias({"to", "recipientId", "targetReceiverId", "receiverId", "user", "userId"})
    private String recipient;

    @NotBlank(message = "Message text is required")
    @JsonAlias({"content", "message", "text"})
    private String messageText;

    public CreateConversationRequest() {}

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMessageText() {
        return messageText;
    }

    public String getContent() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }
}

