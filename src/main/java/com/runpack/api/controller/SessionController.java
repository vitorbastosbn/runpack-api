package com.runpack.api.controller;

import com.runpack.api.dto.request.CreateSessionRequest;
import com.runpack.api.dto.request.FinishSessionRequest;
import com.runpack.api.dto.response.ActiveRunResponse;
import com.runpack.api.dto.response.SessionDetailResponse;
import com.runpack.api.dto.response.SessionResponse;
import com.runpack.api.security.CurrentUser;
import com.runpack.api.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping("/active")
    public List<ActiveRunResponse> getActiveRuns(@CurrentUser UUID currentUserId) {
        return sessionService.getActiveRuns(currentUserId);
    }

    @GetMapping("/{id:[0-9a-fA-F-]{36}}")
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
                                               @PathVariable UUID id,
                                               @RequestBody(required = false) FinishSessionRequest stats) {
        return sessionService.finishSession(id, currentUserId, stats);
    }

    @DeleteMapping("/{id}/participants/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveSession(@CurrentUser UUID currentUserId,
                              @PathVariable UUID id) {
        sessionService.leaveSession(id, currentUserId);
    }
}
