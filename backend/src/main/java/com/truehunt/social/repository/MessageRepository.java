package com.truehunt.social.repository;

import com.truehunt.social.model.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findAllByConversationIdOrderByCreatedAtAsc(Long conversationId);
    List<MessageEntity> findAllBySenderIdOrReceiverIdOrderByCreatedAtDesc(String senderId, String receiverId);
    List<MessageEntity> findAllByConversationIdAndReceiverIdAndStatusNot(Long conversationId, String receiverId, String status);
}

