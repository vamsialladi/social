package com.truehunt.dto.response;

import com.truehunt.model.MediaAttachment;
import java.time.Instant;
import java.util.List;

public class MessageResponse {
    private String id;
    private String conversationId;
    private String senderId;
    private String senderName;
    private String body;
    private List<MediaAttachment> attachments;
    private Instant readAt;
    private Instant createdAt;

    public MessageResponse() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<MediaAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MediaAttachment> attachments) {
        this.attachments = attachments;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
