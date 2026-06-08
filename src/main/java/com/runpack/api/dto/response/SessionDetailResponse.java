package com.runpack.api.dto.response;

import java.time.Instant;
import java.util.List;

public record SessionDetailResponse(
    String id,
    String groupId,
    String groupName,
    String status,
    Instant startedAt,
    Instant finishedAt,
    List<ParticipantResponse> participants
) {}
