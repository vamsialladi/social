package com.truehunt.social.repository;

import com.truehunt.social.model.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    List<ConversationEntity> findAllByOrderByUpdatedAtDesc();
    List<ConversationEntity> findAllByMailboxOrderByUpdatedAtDesc(String mailbox);
    List<ConversationEntity> findAllByMailboxIgnoreCaseOrderByUpdatedAtDesc(String mailbox);
    List<ConversationEntity> findAllByUser1IdOrUser2IdOrderByUpdatedAtDesc(String user1Id, String user2Id);
    java.util.Optional<ConversationEntity> findByUser1IdAndUser2Id(String user1Id, String user2Id);
    java.util.Optional<ConversationEntity> findFirstByNameIgnoreCase(String name);
}
