package com.runpack.api.dto.response;

import java.time.Instant;

public record ActiveRunResponse(
    String sessionId,
    String groupId,
    String groupName,
    String creatorId,
    String creatorName,
    String creatorAvatarUrl,
    int participantCount,
    Instant startedAt
) {}
