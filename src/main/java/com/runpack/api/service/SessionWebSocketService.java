package com.runpack.api.service;

import com.runpack.api.entity.Session;
import com.runpack.api.entity.SessionTelemetry;
import com.runpack.api.entity.User;
import com.runpack.api.repository.SessionParticipantRepository;
import com.runpack.api.repository.SessionRepository;
import com.runpack.api.repository.SessionTelemetryRepository;
import com.runpack.api.repository.UserRepository;
import com.runpack.api.websocket.dto.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SessionWebSocketService {

    private static final Set<String> ALLOWED_EMOJIS = Set.of("🔥", "💪", "👏", "😤");
    private static final String RATE_KEY_PREFIX = "ws:rate:";

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionRepository sessionRepository;
    private final SessionParticipantRepository participantRepository;
    private final SessionTelemetryRepository telemetryRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public SessionWebSocketService(SimpMessagingTemplate messagingTemplate,
                                   SessionRepository sessionRepository,
                                   SessionParticipantRepository participantRepository,
                                   SessionTelemetryRepository telemetryRepository,
                                   UserRepository userRepository,
                                   RedisTemplate<String, String> redisTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
        this.telemetryRepository = telemetryRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public void processTelemetry(String sessionId, String userId, TelemetryMessage message) {
        String rateKey = RATE_KEY_PREFIX + sessionId + ":" + userId;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(rateKey, "1", Duration.ofSeconds(1));
        if (!Boolean.TRUE.equals(isNew)) return;

        UUID sid = UUID.fromString(sessionId);
        UUID uid = UUID.fromString(userId);

        Session session = sessionRepository.findById(sid).orElse(null);
        if (session == null || session.getStatus() != Session.Status.active) return;
        if (!participantRepository.existsBySessionIdAndUserId(sid, uid)) return;

        User user = userRepository.findById(uid).orElse(null);
        if (user == null) return;

        SessionTelemetry telemetry = new SessionTelemetry();
        telemetry.setSession(session);
        telemetry.setUser(user);
        telemetry.setElapsedMs(message.elapsedMs() != null ? message.elapsedMs() : 0L);
        telemetry.setDistanceM(message.distanceM() != null ? message.distanceM() : 0.0);
        telemetry.setPaceSKm(message.paceSKm() != null ? message.paceSKm() : 0.0);
        telemetryRepository.save(telemetry);

        publishRanking(sid);
    }

    public void processReaction(String sessionId, String userId, ReactionMessage message) {
        if (!ALLOWED_EMOJIS.contains(message.emoji())) return;

        UUID sid = UUID.fromString(sessionId);
        UUID uid = UUID.fromString(userId);

        if (!participantRepository.existsBySessionIdAndUserId(sid, uid)) return;

        User user = userRepository.findById(uid).orElse(null);
        if (user == null) return;

        String topic = "/topic/session/" + sessionId;
        messagingTemplate.convertAndSend(topic, new ReactionEvent(userId, user.getUsername(), message.emoji()));
    }

    public void publishParticipantJoined(String sessionId, User user) {
        String topic = "/topic/session/" + sessionId;
        messagingTemplate.convertAndSend(topic,
            new ParticipantEvent("participant_joined", user.getId().toString(),
                user.getUsername(), user.getAvatarUrl()));
    }

    public void publishParticipantLeft(String sessionId, User user) {
        String topic = "/topic/session/" + sessionId;
        messagingTemplate.convertAndSend(topic,
            new ParticipantEvent("participant_left", user.getId().toString(),
                user.getUsername(), user.getAvatarUrl()));
    }

    public void publishSessionFinished(String sessionId) {
        String topic = "/topic/session/" + sessionId;
        messagingTemplate.convertAndSend(topic, new SessionFinishedEvent(sessionId));
    }

    private void publishRanking(UUID sessionId) {
        List<SessionTelemetry> latest = telemetryRepository.findLatestPerUserInSession(sessionId);
        latest.sort(Comparator
            .comparingDouble(SessionTelemetry::getDistanceM).reversed()
            .thenComparingLong(SessionTelemetry::getElapsedMs));

        AtomicInteger rank = new AtomicInteger(1);
        List<RankingUpdateEvent.RankingEntry> entries = latest.stream()
            .map(t -> new RankingUpdateEvent.RankingEntry(
                t.getUser().getId().toString(),
                t.getUser().getUsername(),
                t.getUser().getAvatarUrl(),
                rank.getAndIncrement(),
                t.getDistanceM(),
                t.getPaceSKm(),
                t.getElapsedMs()
            ))
            .toList();

        String topic = "/topic/session/" + sessionId;
        messagingTemplate.convertAndSend(topic, new RankingUpdateEvent(entries));
    }
}
