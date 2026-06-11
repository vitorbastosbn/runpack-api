package com.runpack.api.service;

import com.runpack.api.entity.User;
import com.runpack.api.repository.PushTokenRepository;
import com.runpack.api.repository.UserNotificationPreferencesRepository;
import com.runpack.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PushNotificationServiceTests {

    private PushTokenRepository pushTokenRepository;
    private UserNotificationPreferencesRepository prefsRepository;
    private UserRepository userRepository;
    private PushNotificationService service;
    private UUID recipientId;

    @BeforeEach
    void setUp() {
        pushTokenRepository = mock(PushTokenRepository.class);
        prefsRepository = mock(UserNotificationPreferencesRepository.class);
        userRepository = mock(UserRepository.class);
        service = new PushNotificationService(pushTokenRepository, prefsRepository,
            mock(RestTemplate.class), userRepository);
        recipientId = UUID.randomUUID();
    }

    private void stubRecipient(User.Plan plan) {
        User u = new User();
        u.setPlan(plan);
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(u));
    }

    @Test
    void sessionStartedSkippedForFreeRecipient() {
        stubRecipient(User.Plan.free);
        service.notifySessionStarted(recipientId, "Pack", UUID.randomUUID());
        verify(prefsRepository, never()).findByUserId(any());
        verify(pushTokenRepository, never()).findByUserId(any());
    }

    @Test
    void friendRunStartedSkippedForFreeRecipient() {
        stubRecipient(User.Plan.free);
        service.notifyFriendRunStarted(recipientId, "João", UUID.randomUUID());
        verify(prefsRepository, never()).findByUserId(any());
    }

    @Test
    void friendJoinedRunSkippedForFreeRecipient() {
        stubRecipient(User.Plan.free);
        service.notifyFriendJoinedRun(recipientId, "Ana", "João", UUID.randomUUID());
        verify(prefsRepository, never()).findByUserId(any());
    }

    @Test
    void sessionStartedProceedsForPremiumRecipient() {
        stubRecipient(User.Plan.premium);
        service.notifySessionStarted(recipientId, "Pack", UUID.randomUUID());
        verify(prefsRepository).findByUserId(recipientId);
    }

    @Test
    void friendRequestNotGatedByPlan() {
        stubRecipient(User.Plan.free);
        service.notifyFriendRequest(recipientId, "João");
        verify(prefsRepository).findByUserId(recipientId);
    }
}
