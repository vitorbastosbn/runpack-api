package com.runpack.api.dto.response;

public record RunParticipantResult(
    String userId,
    String name,
    String username,
    String avatarUrl,
    double totalDistanceM,
    long totalTimeMs,
    double avgPaceSkm,
    int finalRank
) {}
