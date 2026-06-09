package com.runpack.api.service;

import com.runpack.api.entity.PushToken;
import com.runpack.api.repository.PushTokenRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PushNotificationService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

    private final PushTokenRepository pushTokenRepository;
    private final RestTemplate restTemplate;

    public PushNotificationService(PushTokenRepository pushTokenRepository,
                                   RestTemplate restTemplate) {
        this.pushTokenRepository = pushTokenRepository;
        this.restTemplate = restTemplate;
    }

    @Async
    public void send(UUID recipientId, String title, String body, String deepLink) {
        int hour = ZonedDateTime.now(BRAZIL_ZONE).getHour();
        if (hour >= 23 || hour < 7) return;

        List<PushToken> tokens = pushTokenRepository.findByUserId(recipientId);
        if (tokens.isEmpty()) return;

        tokens.forEach(pt -> {
            try {
                Map<String, Object> payload = Map.of(
                    "to", pt.getToken(),
                    "title", title,
                    "body", body,
                    "data", Map.of("deepLink", deepLink)
                );
                restTemplate.postForObject(EXPO_PUSH_URL, payload, Map.class);
            } catch (Exception ignored) {}
        });
    }

    public void notifyFriendRequest(UUID addresseeId, String requesterName) {
        send(addresseeId, "Nova solicitação",
            requesterName + " quer ser seu amigo",
            "runpack://friends/requests");
    }

    public void notifyFriendAccepted(UUID requesterId, String accepterName) {
        send(requesterId, "Amizade confirmada",
            accepterName + " aceitou seu convite",
            "runpack://friends");
    }

    public void notifySessionStarted(UUID memberId, String groupName, UUID groupId) {
        send(memberId, "Corrida começou! 🏃",
            "O grupo " + groupName + " está correndo agora",
            "runpack://groups/" + groupId);
    }

    public void notifyAchievementUnlocked(UUID userId, String achievementName) {
        send(userId, "Nova conquista! 🏆",
            "Você desbloqueou: " + achievementName,
            "runpack://achievements");
    }

    public void notifyRunResult(UUID userId, double distanceM, long timeMs, UUID sessionId) {
        String distKm = String.format("%.2f", distanceM / 1000);
        String time = formatDuration(timeMs);
        send(userId, "Corrida finalizada",
            "Você correu " + distKm + " km em " + time,
            "runpack://runs/" + sessionId);
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
