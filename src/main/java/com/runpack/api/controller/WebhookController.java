package com.runpack.api.controller;

import com.runpack.api.dto.request.RevenueCatWebhookRequest;
import com.runpack.api.exception.UnauthorizedException;
import com.runpack.api.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private final SubscriptionService subscriptionService;
    private final String webhookSecret;

    public WebhookController(SubscriptionService subscriptionService,
                             @Value("${revenuecat.webhook-secret}") String webhookSecret) {
        this.subscriptionService = subscriptionService;
        this.webhookSecret = webhookSecret;
    }

    /** Sempre retorna 200 para eventos válidos — RevenueCat retenta em qualquer outro status. */
    @PostMapping("/revenuecat")
    public void revenueCat(@RequestHeader(value = "Authorization", required = false) String authorization,
                           @RequestBody RevenueCatWebhookRequest request) {
        if (authorization == null || !authorization.equals(webhookSecret)) {
            throw new UnauthorizedException("Webhook secret inválido");
        }
        if (request.event() == null) return;
        subscriptionService.handleEvent(request.event());
    }
}
