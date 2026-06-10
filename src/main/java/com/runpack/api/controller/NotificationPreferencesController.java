package com.runpack.api.controller;

import com.runpack.api.dto.request.UpdateNotificationPreferencesRequest;
import com.runpack.api.dto.response.NotificationPreferencesResponse;
import com.runpack.api.entity.User;
import com.runpack.api.entity.UserNotificationPreferences;
import com.runpack.api.repository.UserNotificationPreferencesRepository;
import com.runpack.api.repository.UserRepository;
import com.runpack.api.security.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class NotificationPreferencesController {

    private final UserNotificationPreferencesRepository prefsRepository;
    private final UserRepository userRepository;

    public NotificationPreferencesController(UserNotificationPreferencesRepository prefsRepository,
                                              UserRepository userRepository) {
        this.prefsRepository = prefsRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/users/me/notification-preferences")
    public NotificationPreferencesResponse getPreferences(@CurrentUser UUID currentUserId) {
        return prefsRepository.findByUserId(currentUserId)
            .map(this::toResponse)
            .orElseGet(() -> new NotificationPreferencesResponse(true, true, true, true, true, true, true));
    }

    @PatchMapping("/users/me/notification-preferences")
    public NotificationPreferencesResponse updatePreferences(
        @CurrentUser UUID currentUserId,
        @RequestBody UpdateNotificationPreferencesRequest req
    ) {
        UserNotificationPreferences prefs = prefsRepository.findByUserId(currentUserId)
            .orElseGet(() -> {
                UserNotificationPreferences p = new UserNotificationPreferences();
                User user = userRepository.getReferenceById(currentUserId);
                p.setUser(user);
                return p;
            });

        if (req.friendRequest() != null)        prefs.setFriendRequest(req.friendRequest());
        if (req.friendAccepted() != null)       prefs.setFriendAccepted(req.friendAccepted());
        if (req.sessionStarted() != null)       prefs.setSessionStarted(req.sessionStarted());
        if (req.friendRunStarted() != null)     prefs.setFriendRunStarted(req.friendRunStarted());
        if (req.friendJoinedRun() != null)      prefs.setFriendJoinedRun(req.friendJoinedRun());
        if (req.achievementUnlocked() != null)  prefs.setAchievementUnlocked(req.achievementUnlocked());
        if (req.runResult() != null)            prefs.setRunResult(req.runResult());

        return toResponse(prefsRepository.save(prefs));
    }

    private NotificationPreferencesResponse toResponse(UserNotificationPreferences p) {
        return new NotificationPreferencesResponse(
            p.isFriendRequest(),
            p.isFriendAccepted(),
            p.isSessionStarted(),
            p.isFriendRunStarted(),
            p.isFriendJoinedRun(),
            p.isAchievementUnlocked(),
            p.isRunResult()
        );
    }
}
