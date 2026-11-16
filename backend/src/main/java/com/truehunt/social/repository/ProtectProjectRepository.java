package com.truehunt.social.repository;

import com.truehunt.social.model.ProtectProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProtectProjectRepository extends JpaRepository<ProtectProjectEntity, String> {
    List<ProtectProjectEntity> findAllByOrderByUpdatedAtDesc();
}

