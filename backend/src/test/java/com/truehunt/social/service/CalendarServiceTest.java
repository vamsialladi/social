package com.truehunt.social.service;

import com.truehunt.social.dto.CalendarEntryRequest;
import com.truehunt.social.model.CalendarEntryEntity;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.repository.CalendarEntryRepository;
import com.truehunt.social.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CalendarServiceTest {

    @Test
    void sendsReminderAtTheSelectedDateAndTimeOnlyToTheCalendarOwner() {
        CalendarEntryRepository entries = mock(CalendarEntryRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        CalendarService service = new CalendarService(entries, notifications, "Asia/Kolkata");
        LocalDate today = LocalDate.of(2026, 10, 31);
        LocalDateTime dueTime = today.atTime(14, 30);
        CalendarEntryEntity entry = entry(
                "owner@example.com",
                "Team review",
                "Reminder",
                today,
                dueTime,
                "14:30",
                false
        );
        when(entries.findAllByEntryDateGreaterThanEqual(today)).thenReturn(List.of(entry));

        service.sendRemindersFor(dueTime);

        ArgumentCaptor<NotificationEntity> notification = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notifications).save(notification.capture());
        assertEquals("owner@example.com", notification.getValue().getRecipientId());
        assertEquals("calendar_reminder", notification.getValue().getType());
        assertTrue(notification.getValue().getMessage().contains("October 31, 2026"));
        assertTrue(notification.getValue().getMessage().contains("2:30 PM"));
        assertTrue(notification.getValue().getMessage().contains("due now"));
        assertTrue(entry.isReminderSent());
        verify(entries).save(entry);
    }

    @Test
    void doesNotSendBeforeTheSelectedTimeAndSendsOnceItIsDue() {
        CalendarEntryRepository entries = mock(CalendarEntryRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        CalendarService service = new CalendarService(entries, notifications, "Asia/Kolkata");
        LocalDate today = LocalDate.of(2026, 10, 31);
        LocalDateTime dueTime = today.atTime(14, 30);
        CalendarEntryEntity entry = entry(
                "owner@example.com",
                "Team review",
                "Reminder",
                today,
                dueTime,
                "14:30",
                false
        );
        when(entries.findAllByEntryDateGreaterThanEqual(today)).thenReturn(List.of(entry));

        service.sendRemindersFor(dueTime.minusMinutes(1));
        assertTrue(!entry.isReminderSent());
        verifyNoInteractions(notifications);

        service.sendRemindersFor(dueTime);

        verify(notifications).save(any(NotificationEntity.class));
        assertTrue(entry.isReminderSent());
        verify(entries).save(entry);
    }

    @Test
    void completedTasksAreMarkedProcessedWithoutCreatingNotifications() {
        CalendarEntryRepository entries = mock(CalendarEntryRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        CalendarService service = new CalendarService(entries, notifications, "Asia/Kolkata");
        LocalDate today = LocalDate.of(2026, 10, 31);
        LocalDateTime dueTime = today.atTime(14, 30);
        CalendarEntryEntity entry = entry(
                "owner@example.com",
                "Submit edit",
                "Task",
                today,
                dueTime,
                "14:30",
                true
        );
        when(entries.findAllByEntryDateGreaterThanEqual(today)).thenReturn(List.of(entry));

        service.sendRemindersFor(dueTime);

        assertTrue(entry.isReminderSent());
        verify(entries).save(entry);
        verifyNoInteractions(notifications);
    }

    @Test
    void creatingAnEntryForTodayAtAPastSelectedTimeSendsItImmediately() {
        CalendarEntryRepository entries = mock(CalendarEntryRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        CalendarService service = new CalendarService(entries, notifications, zone.getId());
        LocalDate today = LocalDate.now(zone);
        LocalTime selectedTime = LocalTime.now(zone).minusMinutes(5).withSecond(0).withNano(0);
        when(entries.findByOwnerIdAndEntryId("owner@example.com", "entry-1"))
                .thenReturn(Optional.empty());
        when(entries.save(any(CalendarEntryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.saveEntry(
                "Owner@Example.com",
                entryRequest("entry-1", today, selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")))
        );

        ArgumentCaptor<NotificationEntity> notification = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notifications).save(notification.capture());
        assertEquals("owner@example.com", notification.getValue().getRecipientId());
        assertTrue(notification.getValue().getMessage().contains(
                today.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH))
        ));
        assertTrue(notification.getValue().getMessage().contains(
                selectedTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
        ));
        ArgumentCaptor<CalendarEntryEntity> savedEntry = ArgumentCaptor.forClass(CalendarEntryEntity.class);
        verify(entries, times(2)).save(savedEntry.capture());
        assertEquals(
                today.atTime(selectedTime),
                savedEntry.getAllValues().get(0).getReminderDateTime()
        );
    }

    @Test
    void reopeningTaskOnItsScheduledDateAfterTheSelectedTimeCreatesItsReminder() {
        CalendarEntryRepository entries = mock(CalendarEntryRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        CalendarService service = new CalendarService(entries, notifications, zone.getId());
        LocalDate today = LocalDate.now(zone);
        LocalDateTime dueTime = LocalDateTime.now(zone).minusMinutes(1).withSecond(0).withNano(0);
        CalendarEntryEntity entry = entry(
                "owner@example.com",
                "Submit edit",
                "Task",
                today,
                dueTime,
                dueTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                true
        );
        entry.setReminderSent(true);
        when(entries.findByOwnerIdAndEntryId("owner@example.com", "entry-1"))
                .thenReturn(Optional.of(entry));
        when(entries.save(any(CalendarEntryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateCompletion("owner@example.com", "entry-1", false);

        verify(notifications).save(any(NotificationEntity.class));
        assertTrue(entry.isReminderSent());
    }

    @Test
    void updatesPreviouslyStoredOneDayReminderToTheSelectedDateAndTime() {
        CalendarEntryRepository entries = mock(CalendarEntryRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        CalendarService service = new CalendarService(entries, notifications, "Asia/Kolkata");
        LocalDate today = LocalDate.of(2026, 10, 31);
        LocalDate tomorrow = today.plusDays(1);
        CalendarEntryEntity entry = entry(
                "owner@example.com",
                "Team review",
                "Reminder",
                tomorrow,
                today.atTime(14, 30),
                "14:30",
                false
        );
        entry.setReminderSent(true);
        when(entries.findAllByEntryDateGreaterThanEqual(today)).thenReturn(List.of(entry));

        service.sendRemindersFor(today.atTime(12, 0));

        assertEquals(tomorrow.atTime(14, 30), entry.getReminderDateTime());
        assertTrue(!entry.isReminderSent());
        verify(entries).save(entry);
        verifyNoInteractions(notifications);
    }

    private CalendarEntryEntity entry(
            String owner,
            String title,
            String type,
            LocalDate date,
            LocalDateTime reminderDateTime,
            String time,
            boolean completed
    ) {
        CalendarEntryEntity entry = new CalendarEntryEntity();
        entry.setOwnerId(owner);
        entry.setEntryId("entry-1");
        entry.setTitle(title);
        entry.setType(type);
        entry.setEntryDate(date);
        entry.setReminderDateTime(reminderDateTime);
        entry.setTime(time);
        entry.setCompleted(completed);
        return entry;
    }

    private CalendarEntryRequest entryRequest(String id, LocalDate date, String time) {
        CalendarEntryRequest request = new CalendarEntryRequest();
        request.setId(id);
        request.setTitle("Tomorrow note");
        request.setType("Note");
        request.setDate(date);
        request.setTime(time);
        return request;
    }
}
