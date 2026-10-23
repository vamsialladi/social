package com.truehunt.dto.request;

import com.truehunt.model.MediaAttachment;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CreatePostRequest {

    private String type = "post"; // post, article, opportunity

    private String title;

    @NotBlank(message = "Content is required.")
    private String content;

    private List<MediaAttachment> attachments;

    private String visibility = "public";

    public CreatePostRequest() {}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<MediaAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MediaAttachment> attachments) {
        this.attachments = attachments;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }
}
