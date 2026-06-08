package com.runpack.api.dto.response;

import java.time.Instant;

public record ParticipantResponse(
    String userId,
    String name,
    String username,
    String avatarUrl,
    Instant joinedAt,
    Instant leftAt
) {}
