package com.runpack.api.dto.response;

public record AchievementResponse(
    String id,
    String slug,
    String name,
    String description,
    String iconUrl
) {}
