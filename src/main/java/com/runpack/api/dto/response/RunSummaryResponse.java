package com.runpack.api.dto.response;

import java.time.Instant;

public record RunSummaryResponse(
    String sessionId,
    String groupId,
    String groupName,
    Instant startedAt,
    Instant finishedAt,
    double totalDistanceM,
    long totalTimeMs,
    double avgPaceSkm,
    int finalRank,
    int totalParticipants
) {}
