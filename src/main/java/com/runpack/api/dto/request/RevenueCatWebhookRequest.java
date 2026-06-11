package com.runpack.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RevenueCatWebhookRequest(RevenueCatEvent event) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RevenueCatEvent(
        String type,
        @JsonProperty("app_user_id") String appUserId,
        @JsonProperty("expiration_at_ms") Long expirationAtMs
    ) {}
}
