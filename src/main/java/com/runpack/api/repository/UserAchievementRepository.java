package com.runpack.api.repository;

import com.runpack.api.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {

    List<UserAchievement> findByUserIdOrderByUnlockedAtDesc(UUID userId);

    @Query("SELECT COUNT(ua) > 0 FROM UserAchievement ua WHERE ua.user.id = :userId AND ua.achievement.slug = :slug")
    boolean existsByUserIdAndAchievementSlug(@Param("userId") UUID userId, @Param("slug") String slug);

    @Query("SELECT ua FROM UserAchievement ua JOIN FETCH ua.achievement WHERE ua.user.id = :userId AND ua.session.id = :sessionId")
    List<UserAchievement> findByUserIdAndSessionId(@Param("userId") UUID userId, @Param("sessionId") UUID sessionId);
}
