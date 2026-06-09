package com.runpack.api.dto.response;

import java.time.Instant;
import java.util.List;

public record RunDetailResponse(
    String sessionId,
    String groupId,
    String groupName,
    Instant startedAt,
    Instant finishedAt,
    RunParticipantResult myResult,
    List<RunParticipantResult> participants
) {}
