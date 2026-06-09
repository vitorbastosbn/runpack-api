package com.runpack.api.websocket.dto;

public record ParticipantCompletedEvent(String type, String userId, String username) {
    public ParticipantCompletedEvent(String userId, String username) {
        this("participant_completed", userId, username);
    }
}
