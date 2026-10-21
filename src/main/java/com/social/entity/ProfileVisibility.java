package com.social.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProfileVisibility {
    PUBLIC,
    PRIVATE;

    @JsonCreator
    public static ProfileVisibility fromString(String value) {
        if (value == null) {
            return PUBLIC;
        }
        return ProfileVisibility.valueOf(value.trim().toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return this.name().toLowerCase();
    }
}
