package com.runpack.api.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SendFriendshipRequest(@NotNull UUID addresseeId) {}
