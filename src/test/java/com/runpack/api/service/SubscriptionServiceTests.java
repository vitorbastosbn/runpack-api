package com.runpack.api.service;

import com.runpack.api.dto.request.RevenueCatWebhookRequest.RevenueCatEvent;
import com.runpack.api.entity.User;
import com.runpack.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubscriptionServiceTests {

    private UserRepository userRepository;
    private SubscriptionService service;
    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new SubscriptionService(userRepository);
        userId = UUID.randomUUID();
        user = new User();
        user.setPlan(User.Plan.free);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    private RevenueCatEvent event(String type, Long expirationAtMs) {
        return new RevenueCatEvent(type, userId.toString(), expirationAtMs);
    }

    @Test
    void initialPurchaseActivatesPremium() {
        long expMs = Instant.now().plusSeconds(2_592_000).toEpochMilli();
        service.handleEvent(event("INITIAL_PURCHASE", expMs));
        assertThat(user.getPlan()).isEqualTo(User.Plan.premium);
        assertThat(user.getPlanExpiresAt()).isEqualTo(Instant.ofEpochMilli(expMs));
    }

    @Test
    void renewalKeepsPremiumAndExtendsExpiry() {
        long expMs = Instant.now().plusSeconds(5_184_000).toEpochMilli();
        service.handleEvent(event("RENEWAL", expMs));
        assertThat(user.getPlan()).isEqualTo(User.Plan.premium);
        assertThat(user.getPlanExpiresAt()).isEqualTo(Instant.ofEpochMilli(expMs));
    }

    @Test
    void cancellationDoesNotDowngradeImmediately() {
        user.setPlan(User.Plan.premium);
        Instant paid = Instant.now().plusSeconds(864_000);
        user.setPlanExpiresAt(paid);
        service.handleEvent(event("CANCELLATION", null));
        assertThat(user.getPlan()).isEqualTo(User.Plan.premium);
        assertThat(user.getPlanExpiresAt()).isEqualTo(paid);
    }

    @Test
    void expirationDowngradesToFree() {
        user.setPlan(User.Plan.premium);
        service.handleEvent(event("EXPIRATION", null));
        assertThat(user.getPlan()).isEqualTo(User.Plan.free);
        assertThat(user.getPlanExpiresAt()).isNull();
    }

    @Test
    void billingIssueDowngradesToFree() {
        user.setPlan(User.Plan.premium);
        service.handleEvent(event("BILLING_ISSUE", null));
        assertThat(user.getPlan()).isEqualTo(User.Plan.free);
    }

    @Test
    void unknownEventTypeIsIgnored() {
        service.handleEvent(event("SUBSCRIBER_ALIAS", null));
        assertThat(user.getPlan()).isEqualTo(User.Plan.free);
    }

    @Test
    void anonymousAppUserIdIsIgnoredWithoutError() {
        RevenueCatEvent anon = new RevenueCatEvent("INITIAL_PURCHASE", "$RCAnonymousID:abc123", null);
        service.handleEvent(anon);
        assertThat(user.getPlan()).isEqualTo(User.Plan.free);
    }

    @Test
    void unknownUserIsIgnoredWithoutError() {
        RevenueCatEvent ghost = new RevenueCatEvent("INITIAL_PURCHASE", UUID.randomUUID().toString(), null);
        service.handleEvent(ghost);
    }
}
