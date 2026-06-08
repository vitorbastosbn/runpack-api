package com.runpack.api.dto.response;

import java.time.Instant;
import java.util.List;

public record RunDetailResponse(
    String sessionId,
    String groupId,
    String groupName,
    Instant startedAt,
    Instant finishedAt,
    double myTotalDistanceM,
    long myTotalTimeMs,
    double myAvgPaceSkm,
    int myFinalRank,
    List<RunParticipantResult> participants
) {}
