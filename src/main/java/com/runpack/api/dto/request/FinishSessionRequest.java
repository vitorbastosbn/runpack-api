package com.runpack.api.dto.request;

public record FinishSessionRequest(
    Long elapsedMs,
    Double distanceM,
    Double paceSKm
) {}
