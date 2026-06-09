package com.runpack.api.service;

import com.runpack.api.dto.request.FinishSessionRequest;
import com.runpack.api.entity.Group;
import com.runpack.api.entity.RunResult;
import com.runpack.api.entity.Session;
import com.runpack.api.entity.SessionParticipant;
import com.runpack.api.entity.SessionTelemetry;
import com.runpack.api.entity.User;
import com.runpack.api.exception.BadRequestException;
import com.runpack.api.repository.GroupMemberRepository;
import com.runpack.api.repository.GroupRepository;
import com.runpack.api.repository.RunResultRepository;
import com.runpack.api.repository.SessionParticipantRepository;
import com.runpack.api.repository.SessionRepository;
import com.runpack.api.repository.SessionTelemetryRepository;
import com.runpack.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTests {

    private SessionRepository sessionRepository;
    private SessionParticipantRepository participantRepository;
    private SessionTelemetryRepository telemetryRepository;
    private RunResultRepository runResultRepository;
    private UserRepository userRepository;
    private GroupMemberRepository groupMemberRepository;
    private SessionWebSocketService wsService;
    private AchievementService achievementService;
    private PushNotificationService pushService;
    private SessionService service;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SessionRepository.class);
        participantRepository = mock(SessionParticipantRepository.class);
        telemetryRepository = mock(SessionTelemetryRepository.class);
        runResultRepository = mock(RunResultRepository.class);
        GroupRepository groupRepository = mock(GroupRepository.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        userRepository = mock(UserRepository.class);
        wsService = mock(SessionWebSocketService.class);
        achievementService = mock(AchievementService.class);
        pushService = mock(PushNotificationService.class);

        service = new SessionService(
            sessionRepository,
            participantRepository,
            telemetryRepository,
            runResultRepository,
            groupRepository,
            groupMemberRepository,
            userRepository,
            wsService,
            achievementService,
            pushService
        );
    }

    @Test
    void finishGroupSessionWithOnlyCreatorStoresZeroResultAndIgnoresCreatorStats() {
        User creator = user("creator");
        Group group = group(creator);
        Session session = activeSession(creator, group, Instant.now());
        SessionParticipant participant = participant(session, creator);
        FinishSessionRequest stats = new FinishSessionRequest(120_000L, 900.0, 133.0);

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), creator.getId())).thenReturn(Optional.empty());
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(participantRepository.findBySessionId(session.getId())).thenReturn(List.of(participant));
        when(telemetryRepository.findLatestPerUserInSession(session.getId())).thenReturn(new ArrayList<>());
        when(runResultRepository.findBySessionIdAndUserId(session.getId(), creator.getId())).thenReturn(Optional.empty());

        service.finishSession(session.getId(), creator.getId(), stats);

        verify(telemetryRepository, never()).save(any(SessionTelemetry.class));
        ArgumentCaptor<RunResult> resultCaptor = ArgumentCaptor.forClass(RunResult.class);
        verify(runResultRepository).save(resultCaptor.capture());
        RunResult result = resultCaptor.getValue();
        assertThat(result.getTotalDistanceM()).isZero();
        assertThat(result.getTotalTimeMs()).isZero();
        assertThat(result.getAvgPaceSkm()).isZero();
        assertThat(result.getFinalRank()).isEqualTo(1);
    }

    @Test
    void finishInstantSessionWithOnlyCreatorKeepsCreatorStats() {
        User creator = user("creator");
        Session session = activeSession(creator, null, Instant.now());
        SessionParticipant participant = participant(session, creator);
        FinishSessionRequest stats = new FinishSessionRequest(120_000L, 900.0, 133.0);
        SessionTelemetry telemetry = telemetry(session, creator, 120_000L, 900.0, 133.0);

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(participantRepository.findBySessionId(session.getId())).thenReturn(List.of(participant));
        when(telemetryRepository.findLatestPerUserInSession(session.getId())).thenReturn(new ArrayList<>(List.of(telemetry)));
        when(runResultRepository.findBySessionIdAndUserId(session.getId(), creator.getId())).thenReturn(Optional.empty());

        service.finishSession(session.getId(), creator.getId(), stats);

        verify(telemetryRepository).save(any(SessionTelemetry.class));
        ArgumentCaptor<RunResult> resultCaptor = ArgumentCaptor.forClass(RunResult.class);
        verify(runResultRepository).save(resultCaptor.capture());
        RunResult result = resultCaptor.getValue();
        assertThat(result.getTotalDistanceM()).isEqualTo(900.0);
        assertThat(result.getTotalTimeMs()).isEqualTo(120_000L);
        assertThat(result.getAvgPaceSkm()).isEqualTo(120_000 / 1000.0 / 0.9);
        assertThat(result.getFinalRank()).isEqualTo(1);
    }

    @Test
    void joinGroupSessionAfterParticipationWindowFinalizesSoloSessionAndRejectsNewParticipant() {
        User creator = user("creator");
        User lateUser = user("late");
        Group group = group(creator);
        Session session = activeSession(creator, group, Instant.now().minusSeconds(16 * 60));
        SessionParticipant creatorParticipant = participant(session, creator);

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(participantRepository.findBySessionIdAndUserId(session.getId(), lateUser.getId())).thenReturn(Optional.empty());
        when(userRepository.findById(lateUser.getId())).thenReturn(Optional.of(lateUser));
        when(participantRepository.findBySessionId(session.getId())).thenReturn(List.of(creatorParticipant));
        when(telemetryRepository.findLatestPerUserInSession(session.getId())).thenReturn(new ArrayList<>());
        when(runResultRepository.findBySessionIdAndUserId(session.getId(), creator.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.joinSession(session.getId(), lateUser.getId()))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Janela de entrada encerrada");

        assertThat(session.getStatus()).isEqualTo(Session.Status.finished);
        verify(participantRepository, never()).save(any(SessionParticipant.class));
        ArgumentCaptor<RunResult> resultCaptor = ArgumentCaptor.forClass(RunResult.class);
        verify(runResultRepository).save(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getTotalDistanceM()).isZero();
    }

    private User user(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(username + "@example.com");
        user.setName(username);
        user.setUsername(username);
        user.setProvider(User.Provider.google);
        user.setProviderId(username);
        return user;
    }

    private Group group(User creator) {
        Group group = new Group();
        group.setId(UUID.randomUUID());
        group.setName("Group");
        group.setCreator(creator);
        return group;
    }

    private Session activeSession(User creator, Group group, Instant startedAt) {
        Session session = new Session();
        session.setId(UUID.randomUUID());
        session.setCreatedBy(creator);
        session.setGroup(group);
        session.setStatus(Session.Status.active);
        session.setStartedAt(startedAt);
        return session;
    }

    private SessionParticipant participant(Session session, User user) {
        SessionParticipant participant = new SessionParticipant();
        participant.setId(UUID.randomUUID());
        participant.setSession(session);
        participant.setUser(user);
        participant.setJoinedAt(session.getStartedAt());
        return participant;
    }

    private SessionTelemetry telemetry(Session session, User user, long elapsedMs, double distanceM, double paceSKm) {
        SessionTelemetry telemetry = new SessionTelemetry();
        telemetry.setSession(session);
        telemetry.setUser(user);
        telemetry.setElapsedMs(elapsedMs);
        telemetry.setDistanceM(distanceM);
        telemetry.setPaceSKm(paceSKm);
        return telemetry;
    }
}
