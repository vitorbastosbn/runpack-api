package com.runpack.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record FriendshipResponse(
        UUID id,
        FriendUser user,
        String status,
        Instant createdAt
) {
    public record FriendUser(UUID id, String name, String username, String avatarUrl) {}
}
