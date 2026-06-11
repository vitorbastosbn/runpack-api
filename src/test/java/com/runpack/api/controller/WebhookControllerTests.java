package com.runpack.api.controller;

import com.runpack.api.dto.request.RevenueCatWebhookRequest;
import com.runpack.api.dto.request.RevenueCatWebhookRequest.RevenueCatEvent;
import com.runpack.api.exception.UnauthorizedException;
import com.runpack.api.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WebhookControllerTests {

    private static final String SECRET = "Bearer test-webhook-secret";

    private SubscriptionService subscriptionService;
    private WebhookController controller;

    @BeforeEach
    void setUp() {
        subscriptionService = mock(SubscriptionService.class);
        controller = new WebhookController(subscriptionService, SECRET);
    }

    private RevenueCatWebhookRequest request() {
        return new RevenueCatWebhookRequest(
            new RevenueCatEvent("INITIAL_PURCHASE", "user-id", null));
    }

    @Test
    void rejectsWrongSecret() {
        assertThatThrownBy(() -> controller.revenueCat("Bearer wrong", request()))
            .isInstanceOf(UnauthorizedException.class);
        verify(subscriptionService, never()).handleEvent(any());
    }

    @Test
    void rejectsMissingHeader() {
        assertThatThrownBy(() -> controller.revenueCat(null, request()))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void delegatesValidEvent() {
        controller.revenueCat(SECRET, request());
        verify(subscriptionService).handleEvent(any(RevenueCatEvent.class));
    }

    @Test
    void toleratesNullEventBody() {
        controller.revenueCat(SECRET, new RevenueCatWebhookRequest(null));
        verify(subscriptionService, never()).handleEvent(any());
    }
}
