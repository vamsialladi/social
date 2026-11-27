package com.truehunt.social.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(
        name = "birthday_notification_dispatches",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_birthday_notification_dispatch",
                columnNames = {"birthday_owner_user_id", "recipient_id", "notification_date"}
        )
)
public class BirthdayNotificationDispatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "birthday_owner_user_id", nullable = false)
    private String birthdayOwnerUserId;

    @Column(name = "recipient_id", nullable = false)
    private String recipientId;

    @Column(name = "notification_date", nullable = false)
    private LocalDate notificationDate;

    public BirthdayNotificationDispatchEntity() {
    }

    public BirthdayNotificationDispatchEntity(
            String birthdayOwnerUserId,
            String recipientId,
            LocalDate notificationDate
    ) {
        this.birthdayOwnerUserId = birthdayOwnerUserId;
        this.recipientId = recipientId;
        this.notificationDate = notificationDate;
    }

    public Long getId() {
        return id;
    }

    public String getBirthdayOwnerUserId() {
        return birthdayOwnerUserId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public LocalDate getNotificationDate() {
        return notificationDate;
    }
}
