package com.social.dto.response;

import java.util.List;

public class MessagesPageResponse {
    private List<MessageResponse> messages;
    private String nextCursor;
    private boolean hasMore;

    public MessagesPageResponse() {
    }

    public MessagesPageResponse(List<MessageResponse> messages, String nextCursor, boolean hasMore) {
        this.messages = messages;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<MessageResponse> messages;
        private String nextCursor;
        private boolean hasMore;

        public Builder messages(List<MessageResponse> messages) {
            this.messages = messages;
            return this;
        }

        public Builder nextCursor(String nextCursor) {
            this.nextCursor = nextCursor;
            return this;
        }

        public Builder hasMore(boolean hasMore) {
            this.hasMore = hasMore;
            return this;
        }

        public MessagesPageResponse build() {
            return new MessagesPageResponse(messages, nextCursor, hasMore);
        }
    }

    public List<MessageResponse> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageResponse> messages) {
        this.messages = messages;
    }

    public List<MessageResponse> getData() {
        return messages;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}
