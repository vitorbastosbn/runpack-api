package com.runpack.api.websocket;

import com.runpack.api.service.SessionWebSocketService;
import com.runpack.api.websocket.dto.ReactionMessage;
import com.runpack.api.websocket.dto.TelemetryMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class SessionWebSocketController {

    private final SessionWebSocketService wsService;

    public SessionWebSocketController(SessionWebSocketService wsService) {
        this.wsService = wsService;
    }

    @MessageMapping("/session/{sessionId}/telemetry")
    public void handleTelemetry(@DestinationVariable String sessionId,
                                 TelemetryMessage message,
                                 Principal principal) {
        if (principal == null) return;
        wsService.processTelemetry(sessionId, principal.getName(), message);
    }

    @MessageMapping("/session/{sessionId}/reaction")
    public void handleReaction(@DestinationVariable String sessionId,
                                ReactionMessage message,
                                Principal principal) {
        if (principal == null) return;
        wsService.processReaction(sessionId, principal.getName(), message);
    }
}
