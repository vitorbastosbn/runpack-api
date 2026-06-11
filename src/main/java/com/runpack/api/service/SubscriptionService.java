package com.runpack.api.service;

import com.runpack.api.dto.request.RevenueCatWebhookRequest.RevenueCatEvent;
import com.runpack.api.entity.User;
import com.runpack.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final UserRepository userRepository;

    public SubscriptionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void handleEvent(RevenueCatEvent event) {
        Optional<User> maybeUser = resolveUser(event.appUserId());
        if (maybeUser.isEmpty()) {
            log.info("[revenuecat] event={} ignored, no user for app_user_id={}",
                event.type(), event.appUserId());
            return;
        }
        User user = maybeUser.get();

        switch (event.type()) {
            case "INITIAL_PURCHASE", "RENEWAL" -> activatePremium(user, event.expirationAtMs());
            case "EXPIRATION", "BILLING_ISSUE" -> deactivatePremium(user);
            case "CANCELLATION" -> log.info("[revenuecat] cancellation user={} stays premium until {}",
                user.getId(), user.getPlanExpiresAt());
            default -> log.info("[revenuecat] event={} ignored", event.type());
        }
    }

    private void activatePremium(User user, Long expirationAtMs) {
        user.setPlan(User.Plan.premium);
        user.setPlanExpiresAt(expirationAtMs != null ? Instant.ofEpochMilli(expirationAtMs) : null);
        userRepository.save(user);
        log.info("[revenuecat] premium activated user={} expires={}", user.getId(), user.getPlanExpiresAt());
    }

    private void deactivatePremium(User user) {
        user.setPlan(User.Plan.free);
        user.setPlanExpiresAt(null);
        userRepository.save(user);
        log.info("[revenuecat] premium deactivated user={}", user.getId());
    }

    private Optional<User> resolveUser(String appUserId) {
        if (appUserId == null || appUserId.startsWith("$RCAnonymousID:")) return Optional.empty();
        try {
            return userRepository.findById(UUID.fromString(appUserId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
