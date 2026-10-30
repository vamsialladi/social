package com.truehunt.social.repository;

import com.truehunt.social.model.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {
    Optional<ProfileEntity> findByUserId(String userId);
    Optional<ProfileEntity> findByEmail(String email);
}
