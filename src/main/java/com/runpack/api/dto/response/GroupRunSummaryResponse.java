package com.runpack.api.dto.response;

import java.time.Instant;

public record GroupRunSummaryResponse(
    String sessionId,
    Instant finishedAt,
    int participantCount,
    Double distanceGoalM,
    String winnerName,
    String winnerUsername,
    String winnerAvatarUrl,
    Double winnerDistanceM
) {}
