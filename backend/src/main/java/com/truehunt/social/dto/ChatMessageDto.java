package com.truehunt.social.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ChatMessageDto {

    private String id;
    private Long conversationId;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String receiverName;
    private String content;
    private String direction;
    private Object attachment;
    private String status = "SENT";
    private String sentAt;
    private String createdAt;
    private String type = "CHAT"; // CHAT, JOIN, LEAVE, TYPING

    public ChatMessageDto() {}

    public ChatMessageDto(String id, Long conversationId, String senderId, String senderName,
                          String receiverId, String receiverName, String content, String direction,
                          Object attachment, String status) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.content = content;
        this.direction = direction;
        this.attachment = attachment;
        this.status = status;
        this.sentAt = LocalDateTime.now().toString();
        this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public Object getAttachment() { return attachment; }
    public void setAttachment(Object attachment) { this.attachment = attachment; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}

