package com.runpack.api.dto.response;

import java.time.Instant;

public record ActiveRunResponse(
    String sessionId,
    String groupId,
    String groupName,
    int participantCount,
    Instant startedAt
) {}
