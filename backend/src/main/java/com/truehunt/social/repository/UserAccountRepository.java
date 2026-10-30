package com.truehunt.social.repository;

import com.truehunt.social.model.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {
    Optional<UserAccountEntity> findByEmail(String email);
    Optional<UserAccountEntity> findByPublicUserId(String publicUserId);
    boolean existsByPublicUserId(String publicUserId);
}
