package com.truehunt.social.repository;

import com.truehunt.social.model.SupportTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicketEntity, String> {
    List<SupportTicketEntity> findAllByOrderByCreatedAtDesc();
}

