package com.truehunt.dto.request;

import com.truehunt.model.MediaAttachment;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class SendMessageRequest {

    @NotBlank(message = "Message body cannot be empty.")
    private String body;

    private List<MediaAttachment> attachments;

    public SendMessageRequest() {}

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<MediaAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MediaAttachment> attachments) {
        this.attachments = attachments;
    }
}
