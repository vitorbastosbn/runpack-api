package com.runpack.api.service;

import com.runpack.api.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Auto-finishes sessions that have been active but idle (no telemetry) for too long —
 * e.g. someone starts a run and nobody actually runs.
 */
@Service
public class SessionCleanupService {

    private static final Logger log = LoggerFactory.getLogger(SessionCleanupService.class);
    private static final Duration IDLE_LIMIT = Duration.ofMinutes(15);

    private final SessionRepository sessionRepository;
    private final SessionService sessionService;

    public SessionCleanupService(SessionRepository sessionRepository, SessionService sessionService) {
        this.sessionRepository = sessionRepository;
        this.sessionService = sessionService;
    }

    @Scheduled(fixedDelay = 60_000) // every minute
    public void finishIdleSessions() {
        Instant cutoff = Instant.now().minus(IDLE_LIMIT);
        List<Object[]> idle = sessionRepository.findIdleActiveSessions(cutoff);
        if (idle.isEmpty()) {
            return;
        }
        for (Object[] row : idle) {
            UUID sessionId = (UUID) row[0];
            UUID creatorId = (UUID) row[1];
            try {
                // REQUIRES_NEW: each finish runs in its own transaction.
                sessionService.tryAutoFinish(sessionId, creatorId);
                log.info("[cleanup] auto-finished idle session {}", sessionId);
            } catch (Exception e) {
                log.warn("[cleanup] failed to finish idle session {}: {}", sessionId, e.getMessage());
            }
        }
    }
}
