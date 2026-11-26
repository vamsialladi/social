package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateEndorsementRequest {
    private String receiverProfileId;
    private String endorsementMessage;
    private Map<String, Object> details;

    public CreateEndorsementRequest() {}

    public String getReceiverProfileId() { return receiverProfileId; }
    public void setReceiverProfileId(String receiverProfileId) { this.receiverProfileId = receiverProfileId; }

    public String getEndorsementMessage() { return endorsementMessage; }
    public void setEndorsementMessage(String endorsementMessage) { this.endorsementMessage = endorsementMessage; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}
