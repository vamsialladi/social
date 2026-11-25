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

    @Column(name = "connection_group_id")
    private Long connectionGroupId;

    @Lob
    @Column(name = "group_participant_ids", columnDefinition = "TEXT")
    private String groupParticipantIdsJson = "[]";

    @Lob
    @Column(name = "group_hidden_participant_ids", columnDefinition = "TEXT")
    private String groupHiddenParticipantIdsJson = "[]";

    @Lob
    @Column(name = "group_read_at", columnDefinition = "TEXT")
    private String groupReadAtJson = "{}";

    @Lob
    @Column(name = "group_cleared_at", columnDefinition = "TEXT")
    private String groupClearedAtJson = "{}";

    private String groupImageUrl;

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

    private boolean user1Hidden = false;
    private boolean user2Hidden = false;
    private LocalDateTime user1HiddenAt;
    private LocalDateTime user2HiddenAt;

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

    public Long getConnectionGroupId() { return connectionGroupId; }
    public void setConnectionGroupId(Long connectionGroupId) { this.connectionGroupId = connectionGroupId; }

    public boolean isGroupConversation() { return connectionGroupId != null; }

    public List<String> getGroupParticipantIds() {
        return readJsonList(groupParticipantIdsJson);
    }

    public void setGroupParticipantIds(List<String> participantIds) {
        groupParticipantIdsJson = writeJson(participantIds == null ? List.of() : participantIds);
    }

    public List<String> getGroupHiddenParticipantIds() {
        return readJsonList(groupHiddenParticipantIdsJson);
    }

    public void setGroupHiddenParticipantIds(List<String> participantIds) {
        groupHiddenParticipantIdsJson = writeJson(participantIds == null ? List.of() : participantIds);
    }

    public Map<String, String> getGroupReadAt() {
        if (groupReadAtJson == null || groupReadAtJson.isBlank()) return new HashMap<>();
        try {
            return MAPPER.readValue(groupReadAtJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public void setGroupReadAt(Map<String, String> readAt) {
        groupReadAtJson = writeJson(readAt == null ? Map.of() : readAt);
    }

    public Map<String, String> getGroupClearedAt() {
        return readJsonMap(groupClearedAtJson);
    }

    public void setGroupClearedAt(Map<String, String> clearedAt) {
        groupClearedAtJson = writeJson(clearedAt == null ? Map.of() : clearedAt);
    }

    public String getGroupImageUrl() { return groupImageUrl; }
    public void setGroupImageUrl(String groupImageUrl) { this.groupImageUrl = groupImageUrl; }

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

    public boolean isUser1Hidden() { return user1Hidden; }
    public void setUser1Hidden(boolean user1Hidden) { this.user1Hidden = user1Hidden; }

    public boolean isUser2Hidden() { return user2Hidden; }
    public void setUser2Hidden(boolean user2Hidden) { this.user2Hidden = user2Hidden; }

    public LocalDateTime getUser1HiddenAt() { return user1HiddenAt; }
    public void setUser1HiddenAt(LocalDateTime user1HiddenAt) { this.user1HiddenAt = user1HiddenAt; }

    public LocalDateTime getUser2HiddenAt() { return user2HiddenAt; }
    public void setUser2HiddenAt(LocalDateTime user2HiddenAt) { this.user2HiddenAt = user2HiddenAt; }

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

        if (isGroupConversation()) {
            displayName = name != null ? name : "";
            displayInitials = computeInitials(displayName);
        } else if (isUser1 && user2Name != null && !user2Name.isBlank()) {
            displayName = user2Name;
            displayInitials = computeInitials(user2Name);
        } else if (isUser2 && user1Name != null && !user1Name.isBlank()) {
            displayName = user1Name;
            displayInitials = computeInitials(user1Name);
        }

        dto.put("name", displayName);
        dto.put("initials", displayInitials);
        dto.put("color", color != null ? color : "blue");
        List<Map<String, Object>> thread = getThread();
        boolean effectiveUnread = this.unread;
        if (!cleanIdentifiers.isEmpty() && !thread.isEmpty()) {
            Map<String, Object> lastMsg = thread.get(thread.size() - 1);
            Object lastSender = lastMsg.get("senderId");
            Object lastReceiver = lastMsg.get("receiverId");
            String lastSenderClean = lastSender != null ? String.valueOf(lastSender).trim().toLowerCase(Locale.ROOT) : "";
            String lastReceiverClean = lastReceiver != null ? String.valueOf(lastReceiver).trim().toLowerCase(Locale.ROOT) : "";

            if (isGroupConversation()) {
                effectiveUnread = !lastSenderClean.isEmpty()
                        && !cleanIdentifiers.contains(lastSenderClean)
                        && isGroupMessageUnread(lastMsg, cleanIdentifiers);
            } else if (!lastSenderClean.isEmpty() && cleanIdentifiers.contains(lastSenderClean)) {
                effectiveUnread = false;
            } else if (!lastReceiverClean.isEmpty() && cleanIdentifiers.contains(lastReceiverClean)) {
                boolean isLastRead = "READ".equalsIgnoreCase(String.valueOf(lastMsg.get("status")))
                        || lastMsg.get("readAt") != null;
                effectiveUnread = !isLastRead;
            }
        }

        dto.put("preview", preview != null ? preview : "");
        dto.put("date", date != null ? date : "");
        dto.put("updatedAt", updatedAt != null ? updatedAt.toString() : "");
        dto.put("category", category != null ? category : "Connections");
        dto.put("unread", effectiveUnread);
        dto.put("sponsored", sponsored);
        dto.put("starred", starred);
        dto.put("subject", subject != null ? subject : "");
        dto.put("description", description != null ? description : "");
        dto.put("mailbox", mailbox != null ? mailbox : "Focused");
        dto.put("user1Id", user1Id);
        dto.put("user2Id", user2Id);
        dto.put("user1Name", user1Name);
        dto.put("user2Name", user2Name);
        dto.put("isGroupConversation", isGroupConversation());
        if (isGroupConversation()) {
            dto.put("connectionGroupId", connectionGroupId);
            dto.put("groupParticipantIds", getGroupParticipantIds());
            dto.put("groupImageUrl", groupImageUrl);
        }

        LocalDateTime callerHiddenAt = isGroupConversation()
                ? getGroupCutoff(cleanIdentifiers)
                : isUser1 ? user1HiddenAt : (isUser2 ? user2HiddenAt : null);

        if (!cleanIdentifiers.isEmpty()) {
            List<Map<String, Object>> adaptedThread = new ArrayList<>();
            for (Map<String, Object> msg : thread) {
                if (callerHiddenAt != null && isMessageBeforeCutoff(msg, callerHiddenAt)) {
                    continue;
                }
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
            if (callerHiddenAt != null) {
                if (adaptedThread.isEmpty()) {
                    dto.put("preview", "");
                } else {
                    Object lastContent = adaptedThread.get(adaptedThread.size() - 1).get("content");
                    if (lastContent != null) {
                        dto.put("preview", String.valueOf(lastContent));
                    }
                }
            }
        } else {
            dto.put("thread", thread);
        }

        return dto;
    }

    private boolean isGroupMessageUnread(Map<String, Object> message, Set<String> callerIdentifiers) {
        LocalDateTime sentAt;
        try {
            sentAt = LocalDateTime.parse(String.valueOf(message.get("sentAt")));
        } catch (Exception e) {
            return true;
        }
        LocalDateTime cutoff = getGroupCutoff(callerIdentifiers);
        if (cutoff != null && !sentAt.isAfter(cutoff)) return false;

        Map<String, String> readAt = getGroupReadAt();
        String callerReadAt = readAt.entrySet().stream()
                .filter(entry -> callerIdentifiers.contains(entry.getKey().trim().toLowerCase(Locale.ROOT)))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (callerReadAt == null) return true;
        try {
            return sentAt.isAfter(LocalDateTime.parse(callerReadAt));
        } catch (Exception e) {
            return true;
        }
    }

    private LocalDateTime getGroupCutoff(Set<String> callerIdentifiers) {
        return getGroupClearedAt().entrySet().stream()
                .filter(entry -> callerIdentifiers.contains(entry.getKey().trim().toLowerCase(Locale.ROOT)))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .map(value -> {
                    try {
                        return LocalDateTime.parse(value);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private List<String> readJsonList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Map<String, String> readJsonMap(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String writeJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private boolean isMessageBeforeCutoff(Map<String, Object> msg, LocalDateTime cutoff) {
        Object sentAtObj = msg.get("sentAt");
        if (sentAtObj == null) return false;
        try {
            LocalDateTime sentAt = LocalDateTime.parse(String.valueOf(sentAtObj));
            return !sentAt.isAfter(cutoff);
        } catch (Exception ignored) {
            return false;
        }
    }

    private String computeInitials(String title) {
        if (title == null || title.isBlank()) return "TH";
        String[] parts = title.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
