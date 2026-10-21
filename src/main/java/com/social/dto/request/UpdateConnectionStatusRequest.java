package com.social.dto.request;

import com.social.entity.ConnectionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConnectionStatusRequest {

    @NotNull(message = "status is required (accepted, rejected, or blocked)")
    private ConnectionStatus status;
}
