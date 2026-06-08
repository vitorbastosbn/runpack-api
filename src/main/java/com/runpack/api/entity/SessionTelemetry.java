package com.runpack.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "session_telemetry")
@Getter @Setter @NoArgsConstructor
public class SessionTelemetry {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "elapsed_ms", nullable = false)
    private Long elapsedMs;

    @Column(name = "distance_m", nullable = false)
    private Double distanceM;

    @Column(name = "pace_s_km", nullable = false)
    private Double paceSKm;

    @Column(nullable = false)
    private Instant recordedAt;

    @PrePersist
    void onCreate() {
        if (recordedAt == null) recordedAt = Instant.now();
    }
}
