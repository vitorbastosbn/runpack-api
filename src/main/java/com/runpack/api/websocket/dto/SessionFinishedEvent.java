package com.runpack.api.websocket.dto;

public record SessionFinishedEvent(String type, String sessionId) {
    public SessionFinishedEvent(String sessionId) {
        this("session_finished", sessionId);
    }
}
