package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CommentRequest {
    @NotBlank(message = "Comment text cannot be empty")
    @JsonAlias({"content", "message", "comment", "body"})
    private String text;

    @JsonAlias({"name", "author"})
    private String authorName;

    @JsonAlias({"avatar", "profileImageUrl"})
    private String authorAvatar;

    public CommentRequest() {}

    public CommentRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public String getContent() {
        return text;
    }

    public String getMessage() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorAvatar() {
        return authorAvatar;
    }

    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = authorAvatar;
    }
}

