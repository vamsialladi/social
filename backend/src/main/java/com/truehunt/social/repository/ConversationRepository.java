package com.truehunt.social.repository;

import com.truehunt.social.model.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    List<ConversationEntity> findAllByOrderByUpdatedAtDesc();
    List<ConversationEntity> findAllByMailboxOrderByUpdatedAtDesc(String mailbox);
}
