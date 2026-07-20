package com.tourist.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tourist.server.model.InterviewSession;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {
    Optional<InterviewSession> findFirstByOrderByCreationTimestampDesc();

    Optional<InterviewSession> findByCandidateId(UUID candidateId);
}