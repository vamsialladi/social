package com.truehunt.social.repository;

import com.truehunt.social.model.LcmBetaSignupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LcmBetaSignupRepository extends JpaRepository<LcmBetaSignupEntity, Long> {
    Optional<LcmBetaSignupEntity> findByEmailIgnoreCase(String email);
}
