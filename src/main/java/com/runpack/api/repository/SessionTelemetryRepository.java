package com.runpack.api.repository;

import com.runpack.api.entity.SessionTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionTelemetryRepository extends JpaRepository<SessionTelemetry, UUID> {

    Optional<SessionTelemetry> findTopBySessionIdAndUserIdOrderByRecordedAtDesc(UUID sessionId, UUID userId);

    @Query("""
        SELECT t FROM SessionTelemetry t
        WHERE t.session.id = :sessionId
          AND t.recordedAt = (
            SELECT MAX(t2.recordedAt) FROM SessionTelemetry t2
            WHERE t2.session.id = :sessionId AND t2.user.id = t.user.id
          )
    """)
    List<SessionTelemetry> findLatestPerUserInSession(@Param("sessionId") UUID sessionId);
}
