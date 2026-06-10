package com.runpack.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateFriendFavoriteRequest(@NotNull Boolean favorite) {}
