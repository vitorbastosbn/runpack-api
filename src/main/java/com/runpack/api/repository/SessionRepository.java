package com.runpack.api.repository;

import com.runpack.api.entity.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByGroupIdAndStatus(UUID groupId, Session.Status status);

    Optional<Session> findTopByGroupIdAndStatusOrderByFinishedAtDesc(UUID groupId, Session.Status status);

    java.util.List<Session> findByGroupIdAndStatusOrderByFinishedAtDesc(UUID groupId, Session.Status status);

    @Query("""
        SELECT s FROM Session s
        JOIN SessionParticipant sp ON sp.session.id = s.id
        WHERE sp.user.id = :userId AND s.status = 'finished'
        ORDER BY s.finishedAt DESC
    """)
    Page<Session> findFinishedSessionsByUserId(@Param("userId") UUID userId, Pageable pageable);
}
