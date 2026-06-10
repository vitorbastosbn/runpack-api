package com.runpack.api.dto.response;

public record NotificationPreferencesResponse(
    boolean friendRequest,
    boolean friendAccepted,
    boolean sessionStarted,
    boolean friendRunStarted,
    boolean friendJoinedRun,
    boolean achievementUnlocked,
    boolean runResult
) {}
