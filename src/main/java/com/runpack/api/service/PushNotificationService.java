package com.runpack.api.service;

import com.runpack.api.entity.PushToken;
import com.runpack.api.entity.UserNotificationPreferences;
import com.runpack.api.repository.PushTokenRepository;
import com.runpack.api.repository.UserNotificationPreferencesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@Slf4j
public class PushNotificationService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

    private final PushTokenRepository pushTokenRepository;
    private final UserNotificationPreferencesRepository prefsRepository;
    private final RestTemplate restTemplate;

    public PushNotificationService(PushTokenRepository pushTokenRepository,
                                   UserNotificationPreferencesRepository prefsRepository,
                                   RestTemplate restTemplate) {
        this.pushTokenRepository = pushTokenRepository;
        this.prefsRepository = prefsRepository;
        this.restTemplate = restTemplate;
    }

    @Async
    public void send(UUID recipientId, String title, String body, String deepLink) {
        int hour = ZonedDateTime.now(BRAZIL_ZONE).getHour();
        if (hour >= 23 || hour < 7) {
            log.info("[push] skipped quiet hours recipient={}", recipientId);
            return;
        }

        List<PushToken> tokens = pushTokenRepository.findByUserId(recipientId);
        if (tokens.isEmpty()) {
            log.info("[push] no tokens recipient={}", recipientId);
            return;
        }

        tokens.forEach(pt -> {
            try {
                Map<String, Object> payload = Map.of(
                    "to", pt.getToken(),
                    "title", title,
                    "body", body,
                    "data", Map.of("deepLink", deepLink)
                );
                ResponseEntity<Map> response = restTemplate.postForEntity(EXPO_PUSH_URL, payload, Map.class);
                log.info("[push] sent recipient={} status={} response={}",
                    recipientId, response.getStatusCode(), response.getBody());
            } catch (Exception e) {
                log.warn("[push] failed recipient={} tokenId={}: {}",
                    recipientId, pt.getId(), e.getMessage());
            }
        });
    }

    public void notifyFriendRequest(UUID addresseeId, String requesterName) {
        if (!isPushEnabled(addresseeId, UserNotificationPreferences::isFriendRequest)) return;
        send(addresseeId, "Nova solicitação",
            requesterName + " quer ser seu amigo",
            "runpack://friends/requests");
    }

    public void notifyFriendAccepted(UUID requesterId, String accepterName) {
        if (!isPushEnabled(requesterId, UserNotificationPreferences::isFriendAccepted)) return;
        send(requesterId, "Amizade confirmada",
            accepterName + " aceitou seu convite",
            "runpack://friends");
    }

    public void notifySessionStarted(UUID memberId, String groupName, UUID sessionId) {
        if (!isPushEnabled(memberId, UserNotificationPreferences::isSessionStarted)) return;
        send(memberId, "Corrida começou! 🏃",
            "O grupo " + groupName + " está correndo agora",
            "runpack://sessions/" + sessionId);
    }

    public void notifyAchievementUnlocked(UUID userId, String achievementName) {
        if (!isPushEnabled(userId, UserNotificationPreferences::isAchievementUnlocked)) return;
        send(userId, "Nova conquista! 🏆",
            "Você desbloqueou: " + achievementName,
            "runpack://achievements");
    }

    public void notifyFriendRunStarted(UUID recipientId, String creatorName, UUID sessionId) {
        if (!isPushEnabled(recipientId, UserNotificationPreferences::isFriendRunStarted)) return;
        send(recipientId, creatorName + " está correndo!",
            "Entre e corra junto nos próximos 15 min",
            "runpack://sessions/" + sessionId);
    }

    public void notifyFriendJoinedRun(UUID recipientId, String joinerName, String creatorName, UUID sessionId) {
        if (!isPushEnabled(recipientId, UserNotificationPreferences::isFriendJoinedRun)) return;
        send(recipientId, joinerName + " entrou na corrida de " + creatorName,
            "Ainda dá tempo — entre agora!",
            "runpack://sessions/" + sessionId);
    }

    public void notifyRunResult(UUID userId, double distanceM, long timeMs, UUID sessionId) {
        if (!isPushEnabled(userId, UserNotificationPreferences::isRunResult)) return;
        String distKm = String.format("%.2f", distanceM / 1000);
        String time = formatDuration(timeMs);
        send(userId, "Corrida finalizada",
            "Você correu " + distKm + " km em " + time,
            "runpack://runs/" + sessionId);
    }

    private boolean isPushEnabled(UUID userId, Function<UserNotificationPreferences, Boolean> getter) {
        return prefsRepository.findByUserId(userId).map(getter).orElse(true);
    }

    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        if (h > 0) return String.format("%dh %02dm", h, m);
        return String.format("%dm %02ds", m, s);
    }
}
