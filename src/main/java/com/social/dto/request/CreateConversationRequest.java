package com.social.dto.request;

import java.util.List;

public class CreateConversationRequest {
    private String participantId;
    private List<String> participantIds;
    private String initialMessage;

    public CreateConversationRequest() {
    }

    public CreateConversationRequest(String participantId, List<String> participantIds, String initialMessage) {
        this.participantId = participantId;
        this.participantIds = participantIds;
        this.initialMessage = initialMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String participantId;
        private List<String> participantIds;
        private String initialMessage;

        public Builder participantId(String participantId) {
            this.participantId = participantId;
            return this;
        }

        public Builder participantIds(List<String> participantIds) {
            this.participantIds = participantIds;
            return this;
        }

        public Builder initialMessage(String initialMessage) {
            this.initialMessage = initialMessage;
            return this;
        }

        public CreateConversationRequest build() {
            return new CreateConversationRequest(participantId, participantIds, initialMessage);
        }
    }

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public List<String> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds;
    }

    public String getInitialMessage() {
        return initialMessage;
    }

    public void setInitialMessage(String initialMessage) {
        this.initialMessage = initialMessage;
    }
}
