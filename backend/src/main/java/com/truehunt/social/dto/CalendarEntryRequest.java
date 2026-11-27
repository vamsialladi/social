package com.truehunt.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CalendarEntryRequest {
    @NotBlank
    private String id;

    @NotBlank
    private String title;

    @NotBlank
    private String type;

    @NotNull
    private LocalDate date;

    private String time;
    private String details;
    private Boolean completed;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
}
