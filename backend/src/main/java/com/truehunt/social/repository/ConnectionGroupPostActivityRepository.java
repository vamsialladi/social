package com.truehunt.social.repository;

import com.truehunt.social.model.ConnectionGroupPostActivityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionGroupPostActivityRepository extends JpaRepository<ConnectionGroupPostActivityEntity, Long> {
    List<ConnectionGroupPostActivityEntity> findAllByPostIdInOrderByCreatedAtAsc(List<Long> postIds);

    List<ConnectionGroupPostActivityEntity> findAllByPostIdOrderByCreatedAtAsc(Long postId);

    void deleteAllByPostId(Long postId);

    void deleteAllByGroupId(Long groupId);
}
