package com.truehunt.social.repository;

import com.truehunt.social.model.NetworkProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NetworkProfileRepository extends JpaRepository<NetworkProfileEntity, String> {
    List<NetworkProfileEntity> findAllByConnectedTrue();
}
