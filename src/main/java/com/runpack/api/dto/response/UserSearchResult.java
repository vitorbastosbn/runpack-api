package com.runpack.api.dto.response;

import java.util.UUID;

public record UserSearchResult(
        UUID id,
        String name,
        String username,
        String avatarUrl,
        UUID friendshipId,
        String relation
) {}
