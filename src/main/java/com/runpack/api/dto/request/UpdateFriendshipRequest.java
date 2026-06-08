package com.runpack.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateFriendshipRequest(@NotBlank String status) {}
