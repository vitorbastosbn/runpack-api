package com.runpack.api.controller;

import com.runpack.api.dto.response.UserAchievementResponse;
import com.runpack.api.entity.Friendship;
import com.runpack.api.entity.UserAchievement;
import com.runpack.api.exception.ForbiddenException;
import com.runpack.api.repository.FriendshipRepository;
import com.runpack.api.repository.GroupMemberRepository;
import com.runpack.api.repository.UserAchievementRepository;
import com.runpack.api.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class AchievementController {

    private final UserAchievementRepository userAchievementRepository;
    private final FriendshipRepository friendshipRepository;
    private final GroupMemberRepository groupMemberRepository;

    public AchievementController(UserAchievementRepository userAchievementRepository,
                                 FriendshipRepository friendshipRepository,
                                 GroupMemberRepository groupMemberRepository) {
        this.userAchievementRepository = userAchievementRepository;
        this.friendshipRepository = friendshipRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @GetMapping("/users/me/achievements")
    public List<UserAchievementResponse> getMyAchievements(@CurrentUser UUID currentUserId) {
        return userAchievementRepository.findByUserIdOrderByUnlockedAtDesc(currentUserId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @GetMapping("/users/{id}/achievements")
    public List<UserAchievementResponse> getUserAchievements(@PathVariable UUID id,
                                                              @CurrentUser UUID currentUserId) {
        if (!id.equals(currentUserId)) {
            boolean areFriends = friendshipRepository.findBetween(currentUserId, id)
                .map(f -> f.getStatus() == Friendship.Status.accepted)
                .orElse(false);
            boolean haveGroupInCommon = groupMemberRepository.haveGroupInCommon(currentUserId, id);
            if (!areFriends && !haveGroupInCommon) {
                throw new ForbiddenException("Perfil privado");
            }
        }
        return userAchievementRepository.findByUserIdOrderByUnlockedAtDesc(id)
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
