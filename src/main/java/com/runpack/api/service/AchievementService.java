package com.runpack.api.service;

import com.runpack.api.entity.Achievement;
import com.runpack.api.entity.Session;
import com.runpack.api.entity.UserAchievement;
import com.runpack.api.repository.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

@Service
public class AchievementService {

    private static final long MIN_TIME_MS = 60_000L;
    private static final double MIN_DISTANCE_M = 0.0;

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final RunResultRepository runResultRepository;
    private final SessionRepository sessionRepository;
    private final SessionParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final PushNotificationService pushService;

    public AchievementService(AchievementRepository achievementRepository,
                               UserAchievementRepository userAchievementRepository,
                               RunResultRepository runResultRepository,
                               SessionRepository sessionRepository,
                               SessionParticipantRepository participantRepository,
                               UserRepository userRepository,
                               PushNotificationService pushService) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.runResultRepository = runResultRepository;
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.pushService = pushService;
    }

    @Async("achievementExecutor")
    @Transactional
    public void evaluateAchievements(UUID userId, UUID sessionId) {
        evaluateFirstRun(userId, sessionId);
        evaluateFirstGroupRun(userId, sessionId);
        evaluateFiveRuns(userId, sessionId);
        evaluateTenKmTotal(userId, sessionId);
        evaluateFiftyKmTotal(userId, sessionId);
        evaluateThreeWeeksStreak(userId, sessionId);
        evaluatePodium(userId, sessionId);
        evaluateFastFive(userId, sessionId);
    }

    private void unlock(UUID userId, UUID sessionId, String slug) {
        if (userAchievementRepository.existsByUserIdAndAchievementSlug(userId, slug)) return;
        Achievement achievement = achievementRepository.findBySlug(slug).orElse(null);
        if (achievement == null) return;

        UserAchievement ua = new UserAchievement();
        ua.setUser(userRepository.getReferenceById(userId));
        ua.setAchievement(achievement);
        ua.setSession(sessionRepository.getReferenceById(sessionId));
        userAchievementRepository.save(ua);

        pushService.notifyAchievementUnlocked(userId, achievement.getName());
    }

    private void evaluateFirstRun(UUID userId, UUID sessionId) {
        if (countValidRuns(userId) >= 1) unlock(userId, sessionId, "first_run");
    }

    private void evaluateFirstGroupRun(UUID userId, UUID sessionId) {
        long participantCount = participantRepository.countBySessionIdAndLeftAtIsNull(sessionId);
        // Count all participants (including those who left)
        long total = participantRepository.findBySessionId(sessionId).size();
        if (total >= 2) unlock(userId, sessionId, "first_group_run");
    }

    private void evaluateFiveRuns(UUID userId, UUID sessionId) {
        if (countValidRuns(userId) >= 5) unlock(userId, sessionId, "five_runs");
    }

    private void evaluateTenKmTotal(UUID userId, UUID sessionId) {
        if (sumDistance(userId) >= 10_000) unlock(userId, sessionId, "ten_km_total");
    }

    private void evaluateFiftyKmTotal(UUID userId, UUID sessionId) {
        if (sumDistance(userId) >= 50_000) unlock(userId, sessionId, "fifty_km_total");
    }

    private void evaluateThreeWeeksStreak(UUID userId, UUID sessionId) {
        List<Instant> dates = runResultRepository.findRunDates(userId, MIN_TIME_MS);
        if (dates.size() < 3) return;

        ZoneId zone = ZoneId.of("America/Sao_Paulo");
        TreeSet<java.time.LocalDate> weeks = new TreeSet<>();
        for (Instant date : dates) {
            java.time.LocalDate weekStart = date.atZone(zone).toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            weeks.add(weekStart);
        }

        List<java.time.LocalDate> weekList = List.copyOf(weeks);
        int maxStreak = 1;
        int streak = 1;
        for (int i = 1; i < weekList.size(); i++) {
            if (weekList.get(i).minusWeeks(1).equals(weekList.get(i - 1))) {
                streak++;
                maxStreak = Math.max(maxStreak, streak);
            } else {
                streak = 1;
            }
        }
        if (maxStreak >= 3) unlock(userId, sessionId, "three_weeks_streak");
    }

    private void evaluatePodium(UUID userId, UUID sessionId) {
        runResultRepository.findBySessionIdAndUserId(sessionId, userId).ifPresent(r -> {
            if (r.getFinalRank() != null && r.getFinalRank() == 1) {
                long total = participantRepository.findBySessionId(sessionId).size();
                if (total >= 3) unlock(userId, sessionId, "podium");
            }
        });
    }

    private void evaluateFastFive(UUID userId, UUID sessionId) {
        runResultRepository.findBySessionIdAndUserId(sessionId, userId).ifPresent(r -> {
            if (r.getTotalDistanceM() >= 5_000 && r.getTotalTimeMs() <= 1_800_000) {
                unlock(userId, sessionId, "fast_five");
            }
        });
    }

    private long countValidRuns(UUID userId) {
        return runResultRepository.countByUserIdAndTotalDistanceMGreaterThanAndTotalTimeMsGreaterThan(
            userId, MIN_DISTANCE_M, MIN_TIME_MS
        );
    }

    private double sumDistance(UUID userId) {
        return runResultRepository.sumDistanceByUserId(userId, MIN_TIME_MS);
    }
}
