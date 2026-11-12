package com.truehunt.social.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Entity
@Table(name = "conversations")
public class ConversationEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user1_id")
    private String user1Id;

    @Column(name = "user2_id")
    private String user2Id;

    private String user1Name;
    private String user2Name;

    private String name;
    private String initials;
    private String color = "blue";
    private String preview;
    private String date;
    private String category = "Connections";
    private boolean unread = false;
    private boolean sponsored = false;
    private boolean starred = false;
    private String subject;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    private String mailbox = "Focused";

    @Lob
    @Column(columnDefinition = "TEXT")
    private String threadJson = "[]";

    private LocalDateTime updatedAt = LocalDateTime.now();

    public ConversationEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUser1Id() { return user1Id; }
    public void setUser1Id(String user1Id) { this.user1Id = user1Id; }

    public String getUser2Id() { return user2Id; }
    public void setUser2Id(String user2Id) { this.user2Id = user2Id; }

    public String getUser1Name() { return user1Name; }
    public void setUser1Name(String user1Name) { this.user1Name = user1Name; }

    public String getUser2Name() { return user2Name; }
    public void setUser2Name(String user2Name) { this.user2Name = user2Name; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isUnread() { return unread; }
    public void setUnread(boolean unread) { this.unread = unread; }

    public boolean isSponsored() { return sponsored; }
    public void setSponsored(boolean sponsored) { this.sponsored = sponsored; }

    public boolean isStarred() { return starred; }
    public void setStarred(boolean starred) { this.starred = starred; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMailbox() { return mailbox; }
    public void setMailbox(String mailbox) { this.mailbox = mailbox; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Map<String, Object>> getThread() {
        if (threadJson == null || threadJson.isBlank()) return new ArrayList<>();
        try {
            return MAPPER.readValue(threadJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setThread(List<Map<String, Object>> thread) {
        if (thread == null) {
            this.threadJson = "[]";
        } else {
            try {
                this.threadJson = MAPPER.writeValueAsString(thread);
            } catch (Exception e) {
                this.threadJson = "[]";
            }
        }
    }

    public void addMessage(String content, String direction, Map<String, Object> attachment) {
        addMessage(null, null, null, content, direction, attachment);
    }

    public void addMessage(String senderId, String senderName, String receiverId,
                           String content, String direction, Object attachment) {
        addMessage(null, senderId, senderName, receiverId, content, direction, attachment);
    }

    public void addMessage(String msgId, String senderId, String senderName, String receiverId,
                           String content, String direction, Object attachment) {
        List<Map<String, Object>> thread = getThread();
        Map<String, Object> msg = new HashMap<>();
        msg.put("id", (msgId != null && !msgId.isBlank()) ? msgId : "msg-" + System.currentTimeMillis() + "-" + thread.size());
        msg.put("content", content);
        msg.put("direction", direction != null ? direction : "outgoing");
        if (senderId != null) msg.put("senderId", senderId);
        if (senderName != null) msg.put("senderName", senderName);
        if (receiverId != null) msg.put("receiverId", receiverId);
        if (attachment != null) {
            msg.put("attachment", attachment);
        }
        msg.put("sentAt", LocalDateTime.now().toString());
        msg.put("createdAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));
        thread.add(msg);
        setThread(thread);
        this.preview = content;
        this.date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd"));
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Object> toDto() {
        return toDto(null, Collections.emptySet());
    }

    public Map<String, Object> toDto(String currentUserId) {
        Set<String> ids = new HashSet<>();
        if (currentUserId != null && !currentUserId.isBlank()) {
            ids.add(currentUserId.trim().toLowerCase(Locale.ROOT));
        }
        return toDto(currentUserId, ids);
    }

    public Map<String, Object> toDto(String currentUserId, Set<String> callerIdentifiers) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", id);

        Set<String> cleanIdentifiers = new HashSet<>();
        if (currentUserId != null && !currentUserId.isBlank()) {
            cleanIdentifiers.add(currentUserId.trim().toLowerCase(Locale.ROOT));
        }
        if (callerIdentifiers != null) {
            for (String cid : callerIdentifiers) {
                if (cid != null && !cid.isBlank()) {
                    cleanIdentifiers.add(cid.trim().toLowerCase(Locale.ROOT));
                }
            }
        }

        String displayName = name != null ? name : "";
        String displayInitials = initials != null ? initials : "";

        String u1Clean = user1Id != null ? user1Id.trim().toLowerCase(Locale.ROOT) : "";
        String u2Clean = user2Id != null ? user2Id.trim().toLowerCase(Locale.ROOT) : "";
        String n1Clean = user1Name != null ? user1Name.trim().toLowerCase(Locale.ROOT) : "";
        String n2Clean = user2Name != null ? user2Name.trim().toLowerCase(Locale.ROOT) : "";

        boolean isUser1 = !u1Clean.isEmpty() && cleanIdentifiers.contains(u1Clean);
        boolean isUser2 = !u2Clean.isEmpty() && cleanIdentifiers.contains(u2Clean);

        if (!isUser1 && !isUser2) {
            if (!n1Clean.isEmpty() && cleanIdentifiers.contains(n1Clean)) isUser1 = true;
            else if (!n2Clean.isEmpty() && cleanIdentifiers.contains(n2Clean)) isUser2 = true;
        }

        if (isUser1 && user2Name != null && !user2Name.isBlank()) {
            displayName = user2Name;
            displayInitials = computeInitials(user2Name);
        } else if (isUser2 && user1Name != null && !user1Name.isBlank()) {
            displayName = user1Name;
            displayInitials = computeInitials(user1Name);
        }

        dto.put("name", displayName);
        dto.put("initials", displayInitials);
        dto.put("color", color != null ? color : "blue");
        dto.put("preview", preview != null ? preview : "");
        dto.put("date", date != null ? date : "");
        dto.put("updatedAt", updatedAt != null ? updatedAt.toString() : "");
        dto.put("category", category != null ? category : "Connections");
        dto.put("unread", unread);
        dto.put("sponsored", sponsored);
        dto.put("starred", starred);
        dto.put("subject", subject != null ? subject : "");
        dto.put("description", description != null ? description : "");
        dto.put("mailbox", mailbox != null ? mailbox : "Focused");
        dto.put("user1Id", user1Id);
        dto.put("user2Id", user2Id);
        dto.put("user1Name", user1Name);
        dto.put("user2Name", user2Name);

        List<Map<String, Object>> thread = getThread();
        if (!cleanIdentifiers.isEmpty()) {
            List<Map<String, Object>> adaptedThread = new ArrayList<>();
            for (Map<String, Object> msg : thread) {
                Map<String, Object> adaptedMsg = new HashMap<>(msg);
                Object msgSenderId = msg.get("senderId");
                Object msgReceiverId = msg.get("receiverId");

                String sId = msgSenderId != null ? String.valueOf(msgSenderId).trim().toLowerCase(Locale.ROOT) : "";
                String rId = msgReceiverId != null ? String.valueOf(msgReceiverId).trim().toLowerCase(Locale.ROOT) : "";

                if (!sId.isEmpty()) {
                    boolean isOutgoing = cleanIdentifiers.contains(sId);
                    adaptedMsg.put("direction", isOutgoing ? "outgoing" : "incoming");
                } else if (!rId.isEmpty()) {
                    boolean isIncoming = cleanIdentifiers.contains(rId);
                    adaptedMsg.put("direction", isIncoming ? "incoming" : "outgoing");
                } else {
                    Object legacyDir = msg.get("direction");
                    if (legacyDir != null) {
                        adaptedMsg.put("direction", legacyDir);
                    }
                }
                adaptedThread.add(adaptedMsg);
            }
            dto.put("thread", adaptedThread);
        } else {
            dto.put("thread", thread);
        }

        return dto;
    }

    private String computeInitials(String title) {
        if (title == null || title.isBlank()) return "TH";
        String[] parts = title.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
