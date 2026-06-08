package com.runpack.api.controller;

import com.runpack.api.dto.request.CreateSessionRequest;
import com.runpack.api.dto.response.SessionDetailResponse;
import com.runpack.api.dto.response.SessionResponse;
import com.runpack.api.security.CurrentUser;
import com.runpack.api.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse createSession(@CurrentUser UUID currentUserId,
                                         @RequestBody CreateSessionRequest request) {
        return sessionService.createSession(currentUserId, request);
    }

    @GetMapping("/{id}")
    public SessionDetailResponse getSession(@CurrentUser UUID currentUserId,
                                            @PathVariable UUID id) {
        return sessionService.getSession(id, currentUserId);
    }

    @PostMapping("/{id}/join")
    public SessionResponse joinSession(@CurrentUser UUID currentUserId,
                                       @PathVariable UUID id) {
        return sessionService.joinSession(id, currentUserId);
    }

    @PostMapping("/{id}/finish")
    public SessionDetailResponse finishSession(@CurrentUser UUID currentUserId,
                                               @PathVariable UUID id) {
        return sessionService.finishSession(id, currentUserId);
    }

    @DeleteMapping("/{id}/participants/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveSession(@CurrentUser UUID currentUserId,
                              @PathVariable UUID id) {
        sessionService.leaveSession(id, currentUserId);
    }
}
