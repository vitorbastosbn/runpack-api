package com.runpack.api.dto.response;

import java.time.Instant;
import java.util.List;

public record GroupLastRunResponse(
    String sessionId,
    Instant finishedAt,
    List<PodiumEntry> podium
) {
    public record PodiumEntry(
        String userId,
        String name,
        String username,
        String avatarUrl,
        double totalDistanceM,
        int finalRank
    ) {}
}
