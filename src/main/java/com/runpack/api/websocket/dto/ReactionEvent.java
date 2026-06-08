package com.runpack.api.websocket.dto;

import java.time.Instant;

public record ReactionEvent(
    String type,
    String fromUserId,
    String fromUsername,
    String emoji,
    Instant sentAt
) {
    public ReactionEvent(String fromUserId, String fromUsername, String emoji) {
        this("reaction", fromUserId, fromUsername, emoji, Instant.now());
    }
}
