package com.social.dto.response;

import java.util.List;

public class ConversationsPageResponse {
    private List<ConversationResponse> conversations;
    private String nextCursor;
    private boolean hasMore;

    public ConversationsPageResponse() {
    }

    public ConversationsPageResponse(List<ConversationResponse> conversations, String nextCursor, boolean hasMore) {
        this.conversations = conversations;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
    }

    public List<ConversationResponse> getConversations() {
        return conversations;
    }

    public void setConversations(List<ConversationResponse> conversations) {
        this.conversations = conversations;
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
