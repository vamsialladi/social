package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePostRequest {
    private String type; // post, opportunity, article
    private String content;
    private String title;
    private String mediaUrl;
    private String mediaType;
    private Map<String, Object> media;
    private Map<String, Object> opportunityDetails;

    public CreatePostRequest() {}

    public String getType() {
        return type != null ? type : "post";
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public Map<String, Object> getMedia() {
        return media;
    }

    public void setMedia(Map<String, Object> media) {
        this.media = media;
    }

    public Map<String, Object> getOpportunityDetails() {
        return opportunityDetails;
    }

    public void setOpportunityDetails(Map<String, Object> opportunityDetails) {
        this.opportunityDetails = opportunityDetails;
    }
}

