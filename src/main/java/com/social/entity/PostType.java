package com.social.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.social.exception.BadRequestException;

public enum PostType {
    POST,
    ARTICLE,
    OPPORTUNITY;

    @JsonCreator
    public static PostType fromString(String value) {
        if (value == null) {
            throw new BadRequestException("Post type cannot be null. Allowed values: post, article, opportunity");
        }
        try {
            return PostType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid post type: '" + value + "'. Allowed values: post, article, opportunity");
        }
    }

    @JsonValue
    public String toValue() {
        return this.name().toLowerCase();
    }
}
