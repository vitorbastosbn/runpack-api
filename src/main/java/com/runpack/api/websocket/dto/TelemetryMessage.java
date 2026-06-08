package com.runpack.api.websocket.dto;

public record TelemetryMessage(
    String sessionId,
    String userId,
    Long elapsedMs,
    Double distanceM,
    Double paceSKm
) {}
