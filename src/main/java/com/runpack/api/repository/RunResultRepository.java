package com.runpack.api.repository;

import com.runpack.api.entity.RunResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RunResultRepository extends JpaRepository<RunResult, UUID> {

    List<RunResult> findBySessionIdOrderByFinalRankAsc(UUID sessionId);

    Optional<RunResult> findBySessionIdAndUserId(UUID sessionId, UUID userId);

    long countByUserIdAndTotalDistanceMGreaterThanAndTotalTimeMsGreaterThan(
        UUID userId, double minDistance, long minTimeMs
    );

    @Query("SELECT COALESCE(SUM(r.totalDistanceM), 0) FROM RunResult r WHERE r.user.id = :userId AND r.totalTimeMs > :minTimeMs")
    double sumDistanceByUserId(@Param("userId") UUID userId, @Param("minTimeMs") long minTimeMs);

    @Query("SELECT COALESCE(SUM(r.totalDistanceM), 0) FROM RunResult r WHERE r.user.id = :userId")
    double sumTotalDistanceByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(r) FROM RunResult r WHERE r.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT MIN(r.avgPaceSkm) FROM RunResult r WHERE r.user.id = :userId AND r.avgPaceSkm > 0")
    Optional<Double> findBestPaceByUserId(@Param("userId") UUID userId);

    @Query("SELECT r.session.finishedAt FROM RunResult r WHERE r.user.id = :userId AND r.totalDistanceM > 0 AND r.totalTimeMs > :minTimeMs ORDER BY r.session.finishedAt ASC")
    List<Instant> findRunDates(@Param("userId") UUID userId, @Param("minTimeMs") long minTimeMs);

    @Query("""
        SELECT r FROM RunResult r
        JOIN FETCH r.session s
        WHERE r.user.id = :userId
          AND s.status = 'finished'
        ORDER BY s.finishedAt DESC
    """)
    List<RunResult> findFinishedRunsByUserId(@Param("userId") UUID userId);

    @Query("""
        SELECT r FROM RunResult r
        JOIN FETCH r.session s
        WHERE r.user.id = :userId
          AND s.finishedAt >= :from
          AND s.finishedAt < :to
    """)
    List<RunResult> findByUserIdAndFinishedAtBetween(
        @Param("userId") UUID userId,
        @Param("from") Instant from,
        @Param("to") Instant to
    );
}
