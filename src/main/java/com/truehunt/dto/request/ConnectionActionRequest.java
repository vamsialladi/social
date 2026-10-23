package com.truehunt.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ConnectionActionRequest {

    @NotBlank(message = "Status is required.")
    @Pattern(regexp = "accepted|rejected|blocked", message = "Status must be 'accepted', 'rejected', or 'blocked'.")
    private String status;

    public ConnectionActionRequest() {}

    public ConnectionActionRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
