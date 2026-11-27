package com.truehunt.social.repository;

import com.truehunt.social.model.BirthdayNotificationDispatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface BirthdayNotificationDispatchRepository
        extends JpaRepository<BirthdayNotificationDispatchEntity, Long> {

    boolean existsByBirthdayOwnerUserIdAndRecipientIdAndNotificationDate(
            String birthdayOwnerUserId,
            String recipientId,
            LocalDate notificationDate
    );
}
