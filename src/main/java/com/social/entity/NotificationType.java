package com.social.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationType {
    CONNECTION_REQUEST("connection_request"),
    CONNECTION_ACCEPTED("connection_accepted"),
    MESSAGE("message"),
    POST_LIKE("post_like"),
    POST_COMMENT("post_comment");

    private final String value;

    NotificationType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static NotificationType fromValue(String text) {
        for (NotificationType type : NotificationType.values()) {
            if (type.value.equalsIgnoreCase(text) || type.name().equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown notification type: " + text);
    }
}
