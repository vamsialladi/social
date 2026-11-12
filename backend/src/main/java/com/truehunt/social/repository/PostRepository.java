package com.truehunt.social.repository;

import com.truehunt.social.model.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, Long> {
    List<PostEntity> findAllByOrderByCreatedAtDesc();
    List<PostEntity> findAllByAuthorIdOrderByCreatedAtDesc(String authorId);
    List<PostEntity> findAllByAuthorId(String authorId);
    List<PostEntity> findAllByAuthorIdIn(Collection<String> authorIds);
    List<PostEntity> findAllByTypeOrderByCreatedAtDesc(String type);
    List<PostEntity> findAllByTypeIgnoreCaseOrderByCreatedAtDesc(String type);
    List<PostEntity> findAllByTypeNotOrderByCreatedAtDesc(String type);
}
