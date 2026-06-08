package com.runpack.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record InviteInfoResponse(
        String type,
        UUID targetId,
        String targetName,
        InvitedBy invitedBy,
        Instant expiresAt
) {
    public record InvitedBy(String name, String username) {}
}
