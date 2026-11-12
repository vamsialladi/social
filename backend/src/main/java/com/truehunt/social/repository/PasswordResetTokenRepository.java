package com.truehunt.social.repository;

import com.truehunt.social.model.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {
    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);
    List<PasswordResetTokenEntity> findByEmailIgnoreCaseAndUsedFalse(String email);
}

