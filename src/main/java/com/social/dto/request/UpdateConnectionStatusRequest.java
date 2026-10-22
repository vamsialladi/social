package com.social.dto.request;

import com.social.entity.ConnectionStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateConnectionStatusRequest {

    @NotNull(message = "status is required (accepted, rejected, or blocked)")
    private ConnectionStatus status;

    public UpdateConnectionStatusRequest() {
    }

    public UpdateConnectionStatusRequest(ConnectionStatus status) {
        this.status = status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }

    public static class Builder {
        private ConnectionStatus status;

        public Builder status(ConnectionStatus status) {
            this.status = status;
            return this;
        }

        public UpdateConnectionStatusRequest build() {
            return new UpdateConnectionStatusRequest(status);
        }
    }
}
