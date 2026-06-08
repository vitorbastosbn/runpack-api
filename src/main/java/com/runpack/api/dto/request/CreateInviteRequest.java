package com.runpack.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateInviteRequest(
        @NotBlank String type,
        @NotNull UUID targetId
) {}
