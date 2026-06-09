package com.runpack.api.service;

import com.runpack.api.dto.request.CreateSessionRequest;
import com.runpack.api.dto.request.FinishSessionRequest;
import com.runpack.api.dto.response.ParticipantResponse;
import com.runpack.api.dto.response.SessionDetailResponse;
import com.runpack.api.dto.response.SessionResponse;
import com.runpack.api.entity.*;
import com.runpack.api.exception.BadRequestException;
import com.runpack.api.exception.ConflictException;
import com.runpack.api.exception.ForbiddenException;
import com.runpack.api.exception.NotFoundException;
import com.runpack.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional(readOnly = true)
public class SessionService {

    private static final int MAX_PARTICIPANTS = 20;

    private final SessionRepository sessionRepository;
    private final SessionParticipantRepository participantRepository;
    private final SessionTelemetryRepository telemetryRepository;
    private final RunResultRepository runResultRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final SessionWebSocketService wsService;
    private final AchievementService achievementService;
    private final PushNotificationService pushService;

    public SessionService(SessionRepository sessionRepository,
                          SessionParticipantRepository participantRepository,
                          SessionTelemetryRepository telemetryRepository,
                          RunResultRepository runResultRepository,
                          GroupRepository groupRepository,
                          GroupMemberRepository groupMemberRepository,
                          UserRepository userRepository,
                          SessionWebSocketService wsService,
                          AchievementService achievementService,
                          PushNotificationService pushService) {
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
        this.telemetryRepository = telemetryRepository;
        this.runResultRepository = runResultRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.wsService = wsService;
        this.achievementService = achievementService;
        this.pushService = pushService;
    }

    @Transactional
    public SessionResponse createSession(UUID creatorId, CreateSessionRequest request) {
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        Group group = null;
        if (request.groupId() != null) {
            UUID groupId = UUID.fromString(request.groupId());
            group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Grupo não encontrado"));
            if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, creatorId)) {
                throw new ForbiddenException("Você não é membro deste grupo");
            }
            if (sessionRepository.findByGroupIdAndStatus(groupId, Session.Status.active).isPresent()) {
                throw new BadRequestException("Já existe uma corrida ativa neste grupo");
            }
        }

        Session session = new Session();
        session.setGroup(group);
        session.setCreatedBy(creator);
        session.setDistanceGoalM(request.distanceGoalM());
        sessionRepository.save(session);

        SessionParticipant participant = new SessionParticipant();
        participant.setSession(session);
        participant.setUser(creator);
        participantRepository.save(participant);

        // Notify group members (except creator) that session started
        if (group != null) {
            final Group finalGroup = group;
            final Session finalSession = session;
            groupMemberRepository.findByGroup_IdOrderByJoinedAtAsc(finalGroup.getId()).stream()
                .filter(m -> !m.getUser().getId().equals(creatorId))
                .forEach(m -> pushService.notifySessionStarted(
                    m.getUser().getId(), finalGroup.getName(), finalGroup.getId()));
        }

        return toResponse(session, participant.getJoinedAt(), creatorId);
    }

    @Transactional
    public SessionResponse joinSession(UUID sessionId, UUID userId) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Sessão não encontrada"));
        if (session.getStatus() == Session.Status.finished) {
            throw new BadRequestException("Sessão já encerrada");
        }

        // Idempotent: if already participant, return existing data (handles re-entry after app restart)
        Optional<SessionParticipant> existing =
            participantRepository.findBySessionIdAndUserId(sessionId, userId);
        if (existing.isPresent()) {
            return toResponse(session, existing.get().getJoinedAt(), userId);
        }
        long activeCount = participantRepository.countBySessionIdAndLeftAtIsNull(sessionId);
        if (activeCount >= MAX_PARTICIPANTS) {
            throw new ConflictException("Sessão com número máximo de participantes");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        SessionParticipant participant = new SessionParticipant();
        participant.setSession(session);
        participant.setUser(user);
        participantRepository.save(participant);

        wsService.publishParticipantJoined(sessionId.toString(), user);

        return toResponse(session, participant.getJoinedAt(), userId);
    }

    public SessionDetailResponse getSession(UUID sessionId, UUID currentUserId) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Sessão não encontrada"));
        if (!participantRepository.existsBySessionIdAndUserId(sessionId, currentUserId)) {
            throw new ForbiddenException("Você não é participante desta sessão");
        }
        return toDetailResponse(session);
    }

    @Transactional
    public SessionDetailResponse finishSession(UUID sessionId, UUID currentUserId, FinishSessionRequest stats) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Sessão não encontrada"));
        if (session.getStatus() == Session.Status.finished) {
            throw new BadRequestException("Sessão já encerrada");
        }

        boolean isCreator = session.getCreatedBy().getId().equals(currentUserId);
        boolean isGroupAdmin = session.getGroup() != null &&
            groupMemberRepository.findByGroup_IdAndUser_Id(session.getGroup().getId(), currentUserId)
                .map(m -> m.getRole() == GroupMember.Role.admin)
                .orElse(false);

        if (!isCreator && !isGroupAdmin) {
            throw new ForbiddenException("Somente o criador ou admin do grupo pode encerrar a corrida");
        }

        // Upsert final telemetry from HTTP body before calculating results
        if (stats != null && stats.distanceM() != null && stats.distanceM() > 0) {
            User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
            SessionTelemetry finalTelemetry = new SessionTelemetry();
            finalTelemetry.setSession(session);
            finalTelemetry.setUser(user);
            finalTelemetry.setElapsedMs(stats.elapsedMs() != null ? stats.elapsedMs() : 0L);
            finalTelemetry.setDistanceM(stats.distanceM());
            finalTelemetry.setPaceSKm(stats.paceSKm() != null ? stats.paceSKm() : 0.0);
            telemetryRepository.save(finalTelemetry);
        }

        Instant finishedAt = Instant.now();
        session.setStatus(Session.Status.finished);
        session.setFinishedAt(finishedAt);

        List<SessionParticipant> active = participantRepository.findBySessionId(sessionId)
            .stream()
            .filter(p -> p.getLeftAt() == null)
            .toList();

        for (SessionParticipant p : active) {
            p.setLeftAt(finishedAt);
        }
        participantRepository.saveAll(active);

        calculateAndSaveResults(session, active);

        wsService.publishSessionFinished(sessionId.toString());

        // Async: evaluate achievements + notify run result for each participant
        active.forEach(p -> {
            achievementService.evaluateAchievements(p.getUser().getId(), session.getId());
            RunResult result = runResultRepository.findBySessionIdAndUserId(session.getId(), p.getUser().getId()).orElse(null);
            if (result != null) {
                pushService.notifyRunResult(p.getUser().getId(),
                    result.getTotalDistanceM(), result.getTotalTimeMs(), session.getId());
            }
        });

        return toDetailResponse(session);
    }

    @Transactional
    public void leaveSession(UUID sessionId, UUID userId) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Sessão não encontrada"));
        if (session.getStatus() == Session.Status.finished) {
            throw new BadRequestException("Sessão já encerrada");
        }

        SessionParticipant participant = participantRepository.findBySessionIdAndUserId(sessionId, userId)
            .orElseThrow(() -> new NotFoundException("Você não é participante desta sessão"));

        participant.setLeftAt(Instant.now());
        participantRepository.save(participant);

        User user = participant.getUser();
        wsService.publishParticipantLeft(sessionId.toString(), user);

        long remaining = participantRepository.countBySessionIdAndLeftAtIsNull(sessionId);
        if (remaining == 0) {
            finishSession(sessionId, session.getCreatedBy().getId(), null);
        }
    }

    /**
     * Called from SessionWebSocketService's outer @Transactional to auto-finish session
     * when all participants hit the distance goal. REQUIRES_NEW ensures that if this call
     * fails (e.g., session already finished by a concurrent request), it does NOT mark
     * the outer transaction as rollback-only — protecting the telemetry save.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void tryAutoFinish(UUID sessionId, UUID creatorId) {
        try {
            finishSession(sessionId, creatorId, null);
        } catch (BadRequestException ignored) {
            // Session already finished by a concurrent goal completion — expected, not an error.
        }
    }

    private void calculateAndSaveResults(Session session, List<SessionParticipant> participants) {
        List<SessionTelemetry> latest = telemetryRepository.findLatestPerUserInSession(session.getId());

        latest.sort(Comparator
            .comparingDouble(SessionTelemetry::getDistanceM).reversed()
            .thenComparingLong(SessionTelemetry::getElapsedMs));

        AtomicInteger rank = new AtomicInteger(1);
        for (SessionTelemetry t : latest) {
            RunResult result = new RunResult();
            result.setSession(session);
            result.setUser(t.getUser());
            result.setTotalDistanceM(t.getDistanceM());
            result.setTotalTimeMs(t.getElapsedMs());
            result.setAvgPaceSkm(t.getDistanceM() > 0
                ? (t.getElapsedMs() / 1000.0) / (t.getDistanceM() / 1000.0)
                : 0.0);
            result.setFinalRank(rank.getAndIncrement());
            runResultRepository.save(result);
        }

        // Participants with no telemetry: derive elapsed from session timestamps
        Instant sessionEnd = session.getFinishedAt() != null ? session.getFinishedAt() : Instant.now();
        int lastRank = latest.size() + 1;
        for (SessionParticipant p : participants) {
            boolean hasTelemetry = latest.stream()
                .anyMatch(t -> t.getUser().getId().equals(p.getUser().getId()));
            if (!hasTelemetry) {
                long elapsedMs = sessionEnd.toEpochMilli() - p.getJoinedAt().toEpochMilli();
                RunResult result = new RunResult();
                result.setSession(session);
                result.setUser(p.getUser());
                result.setTotalDistanceM(0.0);
                result.setTotalTimeMs(Math.max(0L, elapsedMs));
                result.setAvgPaceSkm(0.0);
                result.setFinalRank(lastRank++);
                runResultRepository.save(result);
            }
        }
    }

    private SessionResponse toResponse(Session session, Instant joinedAt, UUID currentUserId) {
        long count = participantRepository.countBySessionIdAndLeftAtIsNull(session.getId());
        return new SessionResponse(
            session.getId().toString(),
            session.getGroup() != null ? session.getGroup().getId().toString() : null,
            session.getGroup() != null ? session.getGroup().getName() : null,
            session.getStatus().name(),
            session.getStartedAt(),
            joinedAt,
            session.getFinishedAt(),
            (int) count,
            participantRepository.existsBySessionIdAndUserId(session.getId(), currentUserId),
            session.getDistanceGoalM()
        );
    }

    private SessionDetailResponse toDetailResponse(Session session) {
        List<ParticipantResponse> participants = participantRepository.findBySessionId(session.getId())
            .stream()
            .map(p -> new ParticipantResponse(
                p.getUser().getId().toString(),
                p.getUser().getName(),
                p.getUser().getUsername(),
                p.getUser().getAvatarUrl(),
                p.getJoinedAt(),
                p.getLeftAt()
            ))
            .toList();

        return new SessionDetailResponse(
            session.getId().toString(),
            session.getGroup() != null ? session.getGroup().getId().toString() : null,
            session.getGroup() != null ? session.getGroup().getName() : null,
            session.getStatus().name(),
            session.getStartedAt(),
            session.getFinishedAt(),
            participants
        );
    }
}
