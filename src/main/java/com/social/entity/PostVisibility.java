package com.social.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.social.exception.BadRequestException;

public enum PostVisibility {
    PUBLIC,
    CONNECTIONS,
    PRIVATE;

    @JsonCreator
    public static PostVisibility fromString(String value) {
        if (value == null) {
            return PUBLIC;
        }
        try {
            return PostVisibility.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid visibility: '" + value + "'. Allowed values: public, connections, private");
        }
    }

    @JsonValue
    public String toValue() {
        return this.name().toLowerCase();
    }
}
