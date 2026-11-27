package com.truehunt.social.service;

import com.truehunt.social.dto.CalendarEntryRequest;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.CalendarEntryEntity;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.repository.CalendarEntryRepository;
import com.truehunt.social.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CalendarService {

    private static final Set<String> ENTRY_TYPES = Set.of("reminder", "task", "note");
    private static final DateTimeFormatter REMINDER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter REMINDER_TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final LocalTime DEFAULT_REMINDER_TIME = LocalTime.of(9, 0);

    private final CalendarEntryRepository calendarEntryRepository;
    private final NotificationRepository notificationRepository;
    private final String reminderTimeZone;

    public CalendarService(
            CalendarEntryRepository calendarEntryRepository,
            NotificationRepository notificationRepository,
            @Value("${app.calendar.reminder-time-zone:Asia/Kolkata}") String reminderTimeZone
    ) {
        this.calendarEntryRepository = calendarEntryRepository;
        this.notificationRepository = notificationRepository;
        this.reminderTimeZone = reminderTimeZone;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEntries(String ownerId) {
        return calendarEntryRepository.findAllByOwnerIdOrderByEntryDateAscTimeAsc(normalizeOwner(ownerId))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public Map<String, Object> saveEntry(String ownerId, CalendarEntryRequest request) {
        String normalizedOwner = normalizeOwner(ownerId);
        String type = normalizeType(request.getType());
        CalendarEntryEntity entry = calendarEntryRepository
                .findByOwnerIdAndEntryId(normalizedOwner, request.getId().trim())
                .orElseGet(CalendarEntryEntity::new);
        String time = blankToNull(request.getTime());
        LocalDateTime reminderDateTime = calculateReminderDateTime(request.getDate(), time);
        boolean reminderChanged = !reminderDateTime.equals(entry.getReminderDateTime());

        entry.setOwnerId(normalizedOwner);
        entry.setEntryId(request.getId().trim());
        entry.setTitle(request.getTitle().trim());
        entry.setType(type);
        entry.setEntryDate(request.getDate());
        entry.setTime(time);
        entry.setReminderDateTime(reminderDateTime);
        entry.setDetails(blankToNull(request.getDetails()));
        if (request.getCompleted() != null) {
            entry.setCompleted(request.getCompleted());
        }
        if (reminderChanged) {
            entry.setReminderSent(false);
        }
        CalendarEntryEntity savedEntry = calendarEntryRepository.save(entry);
        sendReminderIfDue(savedEntry, LocalDateTime.now(ZoneId.of(reminderTimeZone)));
        return toDto(savedEntry);
    }

    @Transactional
    public Map<String, Object> updateCompletion(String ownerId, String entryId, boolean completed) {
        CalendarEntryEntity entry = findOwnedEntry(ownerId, entryId);
        if (!"task".equalsIgnoreCase(entry.getType())) {
            throw new BadRequestException("Only calendar tasks can be marked complete.");
        }
        if (entry.isCompleted() && !completed) {
            entry.setReminderSent(false);
        }
        entry.setCompleted(completed);
        CalendarEntryEntity savedEntry = calendarEntryRepository.save(entry);
        sendReminderIfDue(savedEntry, LocalDateTime.now(ZoneId.of(reminderTimeZone)));
        return toDto(savedEntry);
    }

    @Transactional
    public void deleteEntry(String ownerId, String entryId) {
        calendarEntryRepository.delete(findOwnedEntry(ownerId, entryId));
    }

    @Scheduled(
            cron = "${app.calendar.reminder-cron:0 * * * * *}",
            zone = "${app.calendar.reminder-time-zone:Asia/Kolkata}"
    )
    @Transactional
    public void sendDueReminders() {
        sendRemindersFor(LocalDateTime.now(ZoneId.of(reminderTimeZone)));
    }

    @Transactional
    void sendRemindersFor(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        for (CalendarEntryEntity entry : calendarEntryRepository.findAllByEntryDateGreaterThanEqual(today)) {
            LocalDateTime scheduledDateTime = calculateReminderDateTime(
                    entry.getEntryDate(),
                    blankToNull(entry.getTime())
            );
            boolean scheduleChanged = !scheduledDateTime.equals(entry.getReminderDateTime());
            if (scheduleChanged) {
                entry.setReminderDateTime(scheduledDateTime);
                entry.setReminderSent(false);
            }
            if (!entry.isReminderSent() && !scheduledDateTime.isAfter(now)) {
                sendReminder(entry);
            } else if (scheduleChanged) {
                calendarEntryRepository.save(entry);
            }
        }
    }

    private void sendReminderIfDue(CalendarEntryEntity entry, LocalDateTime now) {
        LocalDateTime scheduledDateTime = calculateReminderDateTime(
                entry.getEntryDate(),
                blankToNull(entry.getTime())
        );
        if (!scheduledDateTime.equals(entry.getReminderDateTime())) {
            entry.setReminderDateTime(scheduledDateTime);
            entry.setReminderSent(false);
        }
        if (!entry.isReminderSent()
                && !scheduledDateTime.isAfter(now)
                && !entry.getEntryDate().isBefore(now.toLocalDate())) {
            sendReminder(entry);
        }
    }

    private void sendReminder(CalendarEntryEntity entry) {
        entry.setReminderSent(true);
        if (!("task".equalsIgnoreCase(entry.getType()) && entry.isCompleted())) {
            String date = entry.getEntryDate().format(REMINDER_DATE_FORMAT);
            String scheduledTime = entry.getTime() == null
                    ? ""
                    : " at " + LocalTime.parse(entry.getTime()).format(REMINDER_TIME_FORMAT);
            NotificationEntity notification = new NotificationEntity(
                    entry.getOwnerId(),
                    "Calendar reminder: " + entry.getTitle(),
                    entry.getType() + " \"" + entry.getTitle() + "\" is scheduled for "
                            + date + scheduledTime + ". Your reminder is due now.",
                    "calendar_reminder"
            );
            notificationRepository.save(notification);
        }
        calendarEntryRepository.save(entry);
    }

    private CalendarEntryEntity findOwnedEntry(String ownerId, String entryId) {
        String normalizedOwner = normalizeOwner(ownerId);
        String normalizedEntryId = entryId == null ? "" : entryId.trim();
        return calendarEntryRepository.findByOwnerIdAndEntryId(normalizedOwner, normalizedEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar entry", "id", entryId));
    }

    private String normalizeType(String type) {
        if (type == null || !ENTRY_TYPES.contains(type.trim().toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Calendar entry type must be Reminder, Task, or Note.");
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private String normalizeOwner(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new BadRequestException("A signed-in user is required for calendar entries.");
        }
        return ownerId.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LocalDateTime calculateReminderDateTime(LocalDate entryDate, String time) {
        LocalTime scheduledTime;
        try {
            scheduledTime = time == null ? DEFAULT_REMINDER_TIME : LocalTime.parse(time);
        } catch (RuntimeException exception) {
            throw new BadRequestException("Calendar entry time must use HH:mm format.");
        }
        return entryDate.atTime(scheduledTime);
    }

    private Map<String, Object> toDto(CalendarEntryEntity entry) {
        return Map.ofEntries(
                Map.entry("id", entry.getEntryId()),
                Map.entry("title", entry.getTitle()),
                Map.entry("type", entry.getType()),
                Map.entry("date", entry.getEntryDate().toString()),
                Map.entry("time", entry.getTime() == null ? "" : entry.getTime()),
                Map.entry("details", entry.getDetails() == null ? "" : entry.getDetails()),
                Map.entry("completed", entry.isCompleted())
        );
    }
}
