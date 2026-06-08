package com.runpack.api.controller;

import com.runpack.api.dto.response.UserAchievementResponse;
import com.runpack.api.entity.UserAchievement;
import com.runpack.api.repository.UserAchievementRepository;
import com.runpack.api.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class AchievementController {

    private final UserAchievementRepository userAchievementRepository;

    public AchievementController(UserAchievementRepository userAchievementRepository) {
        this.userAchievementRepository = userAchievementRepository;
    }

    @GetMapping("/users/me/achievements")
    public List<UserAchievementResponse> getMyAchievements(@CurrentUser UUID currentUserId) {
        return userAchievementRepository.findByUserIdOrderByUnlockedAtDesc(currentUserId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private UserAchievementResponse toResponse(UserAchievement ua) {
        return new UserAchievementResponse(
            ua.getId().toString(),
            ua.getAchievement().getSlug(),
            ua.getAchievement().getName(),
            ua.getAchievement().getDescription(),
            ua.getAchievement().getIconUrl(),
            ua.getSession() != null ? ua.getSession().getId().toString() : null,
            ua.getUnlockedAt()
        );
    }
}
