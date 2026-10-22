package com.social.dto.request;

import java.util.List;

public class MarkNotificationsReadRequest {
    private List<String> notificationIds;

    public MarkNotificationsReadRequest() {
    }

    public MarkNotificationsReadRequest(List<String> notificationIds) {
        this.notificationIds = notificationIds;
    }

    public List<String> getNotificationIds() {
        return notificationIds;
    }

    public void setNotificationIds(List<String> notificationIds) {
        this.notificationIds = notificationIds;
    }
}
