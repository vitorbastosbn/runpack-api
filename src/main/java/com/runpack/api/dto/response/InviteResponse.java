package com.runpack.api.dto.response;

import java.time.Instant;

public record InviteResponse(String token, String url, Instant expiresAt) {}
