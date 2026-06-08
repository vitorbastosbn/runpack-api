package com.runpack.api.websocket.dto;

import java.util.List;

public record RankingUpdateEvent(
    String type,
    List<RankingEntry> rankings
) {
    public record RankingEntry(
        String userId,
        String username,
        String avatarUrl,
        int rank,
        Double distanceM,
        Double paceSKm,
        Long elapsedMs
    ) {}

    public RankingUpdateEvent(List<RankingEntry> rankings) {
        this("ranking_update", rankings);
    }
}
