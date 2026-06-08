package com.runpack.api.dto.response;

import java.time.Instant;

public record UserAchievementResponse(
    String id,
    String slug,
    String name,
    String description,
    String iconUrl,
    String sessionId,
    Instant unlockedAt
) {}
