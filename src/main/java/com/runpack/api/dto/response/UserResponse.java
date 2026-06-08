package com.runpack.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String name,
    String username,
    String email,
    String avatarUrl,
    int totalRuns,
    double totalDistanceM,
    double bestPaceSkm,
    Instant createdAt
) {}
