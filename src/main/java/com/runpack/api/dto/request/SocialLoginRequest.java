package com.runpack.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(
        @NotBlank String provider,
        @NotBlank String idToken
) {}
