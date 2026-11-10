package com.truehunt.social.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "messages")
public class MessageEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    private String senderName;

    @Column(name = "receiver_id", nullable = false)
    private String receiverId;

    private String receiverName;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String direction = "outgoing";

    @Lob
    @Column(columnDefinition = "TEXT")
    private String attachmentJson;

    @Column(nullable = false)
    private String status = "SENT";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public MessageEntity() {}

    public MessageEntity(Long conversationId, String senderId, String senderName,
                         String receiverId, String receiverName, String content, Object attachment) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.content = content;
        setAttachment(attachment);
        this.createdAt = LocalDateTime.now();
        this.status = "SENT";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Object getAttachment() {
        if (attachmentJson == null || attachmentJson.isBlank()) return null;
        try {
            return MAPPER.readValue(attachmentJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return attachmentJson;
        }
    }

    public void setAttachment(Object attachment) {
        if (attachment == null) {
            this.attachmentJson = null;
        } else {
            try {
                this.attachmentJson = MAPPER.writeValueAsString(attachment);
            } catch (Exception e) {
                this.attachmentJson = String.valueOf(attachment);
            }
        }
    }

    public Map<String, Object> toDto(String currentUserId) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", id != null ? "msg-" + id : "msg-" + System.currentTimeMillis());
        dto.put("messageId", id);
        dto.put("conversationId", conversationId);
        dto.put("senderId", senderId);
        dto.put("senderName", senderName);
        dto.put("receiverId", receiverId);
        dto.put("receiverName", receiverName);
        dto.put("content", content);

        boolean isOutgoing = currentUserId != null && (
                currentUserId.equalsIgnoreCase(senderId) ||
                (senderName != null && currentUserId.equalsIgnoreCase(senderName))
        );
        dto.put("direction", isOutgoing ? "outgoing" : "incoming");
        dto.put("status", status);
        dto.put("sentAt", createdAt.toString());
        dto.put("createdAt", createdAt.format(DateTimeFormatter.ofPattern("hh:mm a")));
        if (getAttachment() != null) {
            dto.put("attachment", getAttachment());
        }
        return dto;
    }
}

