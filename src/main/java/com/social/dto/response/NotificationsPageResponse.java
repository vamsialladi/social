package com.social.dto.response;

import java.util.List;

public class NotificationsPageResponse {
    private List<NotificationResponse> notifications;
    private long unreadCount;
    private String nextCursor;
    private boolean hasMore;

    public NotificationsPageResponse() {
    }

    public NotificationsPageResponse(List<NotificationResponse> notifications, long unreadCount,
                                     String nextCursor, boolean hasMore) {
        this.notifications = notifications;
        this.unreadCount = unreadCount;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
    }

    public List<NotificationResponse> getData() {
        return notifications;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<NotificationResponse> notifications;
        private long unreadCount;
        private String nextCursor;
        private boolean hasMore;

        public Builder notifications(List<NotificationResponse> notifications) {
            this.notifications = notifications;
            return this;
        }

        public Builder unreadCount(long unreadCount) {
            this.unreadCount = unreadCount;
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

        public NotificationsPageResponse build() {
            return new NotificationsPageResponse(notifications, unreadCount, nextCursor, hasMore);
        }
    }

    public List<NotificationResponse> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<NotificationResponse> notifications) {
        this.notifications = notifications;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
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
