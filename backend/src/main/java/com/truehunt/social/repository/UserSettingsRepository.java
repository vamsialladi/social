package com.truehunt.social.repository;

import com.truehunt.social.model.UserSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettingsEntity, Long> {
    Optional<UserSettingsEntity> findByUserEmailIgnoreCase(String userEmail);
    void deleteByUserEmailIgnoreCase(String userEmail);
}
