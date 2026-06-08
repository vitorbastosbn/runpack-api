package com.runpack.api.dto.response;

import java.util.UUID;

public record AcceptInviteResponse(String type, UUID targetId) {}
