package com.truehunt.social.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "calendar_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_calendar_entry_owner_id",
                columnNames = {"owner_id", "entry_id"}
        ),
        indexes = @Index(name = "idx_calendar_entry_reminder", columnList = "reminder_date_time,reminder_sent,entry_date")
)
public class CalendarEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long databaseId;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "entry_id", nullable = false)
    private String entryId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String type;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "reminder_date_time")
    private LocalDateTime reminderDateTime;

    private String time;

    @Column(length = 1000)
    private String details;

    private boolean completed;

    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent;

    public Long getDatabaseId() { return databaseId; }
    public void setDatabaseId(Long databaseId) { this.databaseId = databaseId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getEntryId() { return entryId; }
    public void setEntryId(String entryId) { this.entryId = entryId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }

    public LocalDateTime getReminderDateTime() { return reminderDateTime; }
    public void setReminderDateTime(LocalDateTime reminderDateTime) { this.reminderDateTime = reminderDateTime; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }
}
