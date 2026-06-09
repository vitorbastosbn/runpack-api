package com.runpack.api.service;

import com.runpack.api.dto.response.RunDetailResponse;
import com.runpack.api.dto.response.RunParticipantResult;
import com.runpack.api.dto.response.RunSummaryResponse;
import com.runpack.api.entity.RunResult;
import com.runpack.api.entity.Session;
import com.runpack.api.exception.BadRequestException;
import com.runpack.api.exception.ForbiddenException;
import com.runpack.api.exception.NotFoundException;
import com.runpack.api.repository.RunResultRepository;
import com.runpack.api.repository.SessionParticipantRepository;
import com.runpack.api.repository.SessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class HistoryService {

    private final SessionRepository sessionRepository;
    private final SessionParticipantRepository participantRepository;
    private final RunResultRepository runResultRepository;

    public HistoryService(SessionRepository sessionRepository,
                          SessionParticipantRepository participantRepository,
                          RunResultRepository runResultRepository) {
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
        this.runResultRepository = runResultRepository;
    }

    public Page<RunSummaryResponse> getRunHistory(UUID userId, Pageable pageable) {
        Page<Session> sessions = sessionRepository.findFinishedSessionsByUserId(userId, pageable);
        List<RunSummaryResponse> content = sessions.getContent().stream()
            .map(s -> {
                RunResult myResult = runResultRepository.findBySessionIdAndUserId(s.getId(), userId)
                    .orElse(null);
                long totalParticipants = participantRepository.findBySessionId(s.getId()).size();
                return new RunSummaryResponse(
                    s.getId().toString(),
                    s.getGroup() != null ? s.getGroup().getId().toString() : null,
                    s.getGroup() != null ? s.getGroup().getName() : null,
                    s.getStartedAt(),
                    s.getFinishedAt(),
                    myResult != null ? myResult.getTotalDistanceM() : 0.0,
                    myResult != null ? myResult.getTotalTimeMs() : 0L,
                    myResult != null ? myResult.getAvgPaceSkm() : 0.0,
                    myResult != null && myResult.getFinalRank() != null ? myResult.getFinalRank() : 0,
                    (int) totalParticipants
                );
            })
            .toList();
        return new PageImpl<>(content, pageable, sessions.getTotalElements());
    }

    public RunDetailResponse getRunDetail(UUID sessionId, UUID currentUserId) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Sessão não encontrada"));
        if (session.getStatus() != Session.Status.finished) {
            throw new BadRequestException("Sessão ainda não finalizada");
        }
        if (!participantRepository.existsBySessionIdAndUserId(sessionId, currentUserId)) {
            throw new ForbiddenException("Você não participou desta sessão");
        }

        RunResult myResult = runResultRepository.findBySessionIdAndUserId(sessionId, currentUserId)
            .orElse(null);

        List<RunParticipantResult> participants = runResultRepository
            .findBySessionIdOrderByFinalRankAsc(sessionId)
            .stream()
            .map(r -> new RunParticipantResult(
                r.getUser().getId().toString(),
                r.getUser().getName(),
                r.getUser().getUsername(),
                r.getUser().getAvatarUrl(),
                r.getTotalDistanceM(),
                r.getTotalTimeMs(),
                r.getAvgPaceSkm(),
                r.getFinalRank() != null ? r.getFinalRank() : 0
            ))
            .toList();

        RunParticipantResult myResultDto = myResult != null
            ? new RunParticipantResult(
                myResult.getUser().getId().toString(),
                myResult.getUser().getName(),
                myResult.getUser().getUsername(),
                myResult.getUser().getAvatarUrl(),
                myResult.getTotalDistanceM(),
                myResult.getTotalTimeMs(),
                myResult.getAvgPaceSkm(),
                myResult.getFinalRank() != null ? myResult.getFinalRank() : 0)
            : null;

        return new RunDetailResponse(
            session.getId().toString(),
            session.getGroup() != null ? session.getGroup().getId().toString() : null,
            session.getGroup() != null ? session.getGroup().getName() : null,
            session.getStartedAt(),
            session.getFinishedAt(),
            myResultDto,
            participants
        );
    }
}
