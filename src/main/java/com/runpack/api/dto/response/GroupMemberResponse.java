package com.runpack.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record GroupMemberResponse(
        UUID memberId,
        UUID userId,
        String name,
        String username,
        String avatarUrl,
        String role,
        Instant joinedAt
) {}
