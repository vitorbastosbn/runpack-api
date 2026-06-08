package com.runpack.api.websocket.dto;

public record ParticipantEvent(
    String type,
    String userId,
    String username,
    String avatarUrl
) {}
