package com.runpack.api.controller;

import com.runpack.api.dto.request.CreateInviteRequest;
import com.runpack.api.dto.response.AcceptInviteResponse;
import com.runpack.api.dto.response.InviteInfoResponse;
import com.runpack.api.dto.response.InviteResponse;
import com.runpack.api.security.CurrentUser;
import com.runpack.api.service.InviteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/invites")
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @PostMapping
    public ResponseEntity<InviteResponse> createInvite(@CurrentUser UUID userId,
                                                        @Valid @RequestBody CreateInviteRequest request) {
        return ResponseEntity.ok(inviteService.createInvite(userId, request));
    }

    @GetMapping("/{token}")
    public ResponseEntity<InviteInfoResponse> getInviteInfo(@PathVariable String token) {
        return ResponseEntity.ok(inviteService.getInviteInfo(token));
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<AcceptInviteResponse> acceptInvite(@PathVariable String token,
                                                              @CurrentUser UUID userId) {
        return ResponseEntity.ok(inviteService.acceptInvite(token, userId));
    }
}
