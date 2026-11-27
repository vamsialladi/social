package com.truehunt.social.repository;

import com.truehunt.social.model.CalendarEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarEntryRepository extends JpaRepository<CalendarEntryEntity, Long> {
    List<CalendarEntryEntity> findAllByOwnerIdOrderByEntryDateAscTimeAsc(String ownerId);

    Optional<CalendarEntryEntity> findByOwnerIdAndEntryId(String ownerId, String entryId);

    List<CalendarEntryEntity> findAllByEntryDateGreaterThanEqual(LocalDate earliestEntryDate);
}
