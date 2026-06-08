package com.runpack.api.dto.response;

public record AuthResponse(
        String jwt,
        boolean isNewUser,
        String userId,
        String email,
        String username
) {}
