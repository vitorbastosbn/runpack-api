package com.runpack.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank @Size(min = 3, max = 50) String name,
        @Size(max = 200) String description,
        String imageUrl
) {}
