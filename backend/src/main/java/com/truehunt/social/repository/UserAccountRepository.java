package com.truehunt.social.repository;

import com.truehunt.social.model.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {
    Optional<UserAccountEntity> findFirstByEmail(String email);
    default Optional<UserAccountEntity> findByEmail(String email) {
        return findFirstByEmail(email);
    }

    Optional<UserAccountEntity> findFirstByPublicUserId(String publicUserId);
    default Optional<UserAccountEntity> findByPublicUserId(String publicUserId) {
        return findFirstByPublicUserId(publicUserId);
    }

    Optional<UserAccountEntity> findFirstByPhoneNumber(String phoneNumber);
    default Optional<UserAccountEntity> findByPhoneNumber(String phoneNumber) {
        return findFirstByPhoneNumber(phoneNumber);
    }
    default Optional<UserAccountEntity> findByPhone(String phone) {
        return findFirstByPhoneNumber(phone);
    }

    List<UserAccountEntity> findAllByEmail(String email);
    List<UserAccountEntity> findAllByPhoneNumber(String phoneNumber);

    boolean existsByPublicUserId(String publicUserId);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
}
