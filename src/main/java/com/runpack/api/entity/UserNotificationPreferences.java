package com.runpack.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_notification_preferences")
@Getter @Setter @NoArgsConstructor
public class UserNotificationPreferences {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private boolean friendRequest = true;

    @Column(nullable = false)
    private boolean friendAccepted = true;

    @Column(nullable = false)
    private boolean sessionStarted = true;

    @Column(nullable = false)
    private boolean friendRunStarted = true;

    @Column(nullable = false)
    private boolean friendJoinedRun = true;

    @Column(nullable = false)
    private boolean achievementUnlocked = true;

    @Column(nullable = false)
    private boolean runResult = true;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
