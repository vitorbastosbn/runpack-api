package com.runpack.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        String name,
        String description,
        String imageUrl,
        int memberCount,
        String myRole,
        Instant createdAt,
        String activeSessionId
) {}
