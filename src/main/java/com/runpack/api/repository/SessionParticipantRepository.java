package com.runpack.api.repository;

import com.runpack.api.entity.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, UUID> {

    List<SessionParticipant> findBySessionId(UUID sessionId);

    Optional<SessionParticipant> findBySessionIdAndUserId(UUID sessionId, UUID userId);

    boolean existsBySessionIdAndUserId(UUID sessionId, UUID userId);

    long countBySessionIdAndLeftAtIsNull(UUID sessionId);

    @Query("""
        SELECT sp FROM SessionParticipant sp
        JOIN FETCH sp.session s
        WHERE sp.user.id = :userId AND s.status = 'active' AND sp.leftAt IS NULL
    """)
    List<SessionParticipant> findActiveSessionsByUserId(@Param("userId") UUID userId);
}
