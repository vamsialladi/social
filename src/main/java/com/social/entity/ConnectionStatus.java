package com.social.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConnectionStatus {
    PENDING("pending"),
    ACCEPTED("accepted"),
    REJECTED("rejected"),
    BLOCKED("blocked");

    private final String value;

    ConnectionStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ConnectionStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ConnectionStatus status : ConnectionStatus.values()) {
            if (status.value.equalsIgnoreCase(value.trim()) || status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown connection status: " + value + ". Allowed values: pending, accepted, rejected, blocked");
    }
}
