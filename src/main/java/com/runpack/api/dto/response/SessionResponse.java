package com.runpack.api.dto.response;

import java.time.Instant;

public record SessionResponse(
    String id,
    String groupId,
    String groupName,
    String status,
    Instant startedAt,
    Instant joinedAt,
    Instant finishedAt,
    int participantCount,
    boolean isParticipant
) {}
