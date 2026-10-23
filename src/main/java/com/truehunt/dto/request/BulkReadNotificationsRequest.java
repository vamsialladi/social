package com.truehunt.dto.request;

import java.util.List;

public class BulkReadNotificationsRequest {
    private List<String> notificationIds;

    public BulkReadNotificationsRequest() {}

    public List<String> getNotificationIds() {
        return notificationIds;
    }

    public void setNotificationIds(List<String> notificationIds) {
        this.notificationIds = notificationIds;
    }
}
