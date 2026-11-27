package com.truehunt.social.dto;

import jakarta.validation.constraints.NotNull;

public class CalendarEntryCompletionRequest {
    @NotNull
    private Boolean completed;

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
}
