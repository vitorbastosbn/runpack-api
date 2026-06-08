package com.runpack.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "run_result")
@Getter @Setter @NoArgsConstructor
public class RunResult {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_distance_m", nullable = false)
    private Double totalDistanceM = 0.0;

    @Column(name = "total_time_ms", nullable = false)
    private Long totalTimeMs = 0L;

    @Column(name = "avg_pace_s_km", nullable = false)
    private Double avgPaceSkm = 0.0;

    private Integer finalRank;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
