package com.truehunt.social.repository;

import com.truehunt.social.model.ContactMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessageEntity, String> {
    List<ContactMessageEntity> findAllByOrderByCreatedAtDesc();
}
