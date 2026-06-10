package com.runpack.api.dto.request;

public record UpdateNotificationPreferencesRequest(
    Boolean friendRequest,
    Boolean friendAccepted,
    Boolean sessionStarted,
    Boolean friendRunStarted,
    Boolean friendJoinedRun,
    Boolean achievementUnlocked,
    Boolean runResult
) {}
