package com.social.dto.request;

import com.social.entity.Attachment;

import java.util.List;

public class SendMessageRequest {
    private String body;
    private List<Attachment> attachments;

    public SendMessageRequest() {
    }

    public SendMessageRequest(String body, List<Attachment> attachments) {
        this.body = body;
        this.attachments = attachments;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String body;
        private List<Attachment> attachments;

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder attachments(List<Attachment> attachments) {
            this.attachments = attachments;
            return this;
        }

        public SendMessageRequest build() {
            return new SendMessageRequest(body, attachments);
        }
    }

    public boolean isValid() {
        boolean hasBody = body != null && !body.trim().isEmpty();
        boolean hasAttachments = attachments != null && !attachments.isEmpty();
        return hasBody || hasAttachments;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }
}
