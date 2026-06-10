package com.runpack.api.repository;

import com.runpack.api.entity.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByGroupIdAndStatus(UUID groupId, Session.Status status);

    Optional<Session> findTopByGroupIdAndStatusOrderByFinishedAtDesc(UUID groupId, Session.Status status);

    java.util.List<Session> findByGroupIdAndStatusOrderByFinishedAtDesc(UUID groupId, Session.Status status);

    @Query("""
        SELECT s FROM Session s
        JOIN s.group g
        JOIN GroupMember gm ON gm.group.id = g.id
        WHERE gm.user.id = :userId AND s.status = 'active'
        ORDER BY s.startedAt DESC
        """)
    java.util.List<Session> findActiveGroupSessionsForUser(@Param("userId") UUID userId);

    /**
     * Active sessions started before the cutoff with no telemetry recorded after the cutoff
     * (idle/empty). Returns [sessionId, creatorId] rows. Used by the idle-session cleanup job.
     */
    @Query("""
        SELECT s.id, s.createdBy.id FROM Session s
        WHERE s.status = 'active'
          AND s.startedAt < :cutoff
          AND NOT EXISTS (
            SELECT t FROM SessionTelemetry t
            WHERE t.session.id = s.id AND t.recordedAt >= :cutoff
          )
        """)
    java.util.List<Object[]> findIdleActiveSessions(@Param("cutoff") java.time.Instant cutoff);

    @Query("""
        SELECT s FROM Session s
        JOIN SessionParticipant sp ON sp.session.id = s.id
        WHERE sp.user.id = :userId AND s.status = 'finished'
        ORDER BY s.finishedAt DESC
    """)
    Page<Session> findFinishedSessionsByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Session s SET s.group = null WHERE s.group.id = :groupId")
    void clearGroupId(@Param("groupId") UUID groupId);

    @Query("""
        SELECT s FROM Session s
        WHERE s.group IS NULL
          AND s.status = 'active'
          AND s.createdBy.id IN :creatorIds
          AND s.startedAt >= :windowStart
        """)
    List<Session> findActiveSoloSessionsByCreatorIds(
        @Param("creatorIds") List<UUID> creatorIds,
        @Param("windowStart") Instant windowStart);
}
