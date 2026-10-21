package com.social.repository;

import com.social.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, String> {
    Optional<MediaFile> findByObjectKey(String objectKey);
    List<MediaFile> findByOwnerId(String ownerId);
    List<MediaFile> findByPostId(String postId);
    boolean existsByObjectKey(String objectKey);
}
