package com.runpack.api.service;

import com.runpack.api.dto.request.UpdateUserRequest;
import com.runpack.api.dto.response.UserResponse;
import com.runpack.api.dto.response.UserSearchResult;
import com.runpack.api.dto.response.WeeklyStatsEntry;
import com.runpack.api.entity.Friendship;
import com.runpack.api.entity.User;
import com.runpack.api.exception.BadRequestException;
import com.runpack.api.exception.ConflictException;
import com.runpack.api.exception.ForbiddenException;
import com.runpack.api.exception.NotFoundException;
import com.runpack.api.repository.FriendshipRepository;
import com.runpack.api.repository.GroupMemberRepository;
import com.runpack.api.repository.RunResultRepository;
import com.runpack.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final RunResultRepository runResultRepository;

    public UserService(UserRepository userRepository,
                       FriendshipRepository friendshipRepository,
                       GroupMemberRepository groupMemberRepository,
                       RunResultRepository runResultRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.runResultRepository = runResultRepository;
    }

    public UserResponse getMe(UUID userId) {
        return buildProfile(findOrThrow(userId));
    }

    @Transactional
    public UserResponse updateMe(UUID userId, UpdateUserRequest request) {
        User user = findOrThrow(userId);
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name());
        }
        // Avatar comes from Google and is not editable in-app — ignored on purpose.
        if (request.username() != null && !request.username().equals(user.getUsername())) {
            String username = request.username().trim();
            if (username.length() < 3 || username.length() > 20) {
                throw new BadRequestException("Username deve ter entre 3 e 20 caracteres");
            }
            if (!username.matches("[a-zA-Z0-9._#]+")) {
                throw new BadRequestException("Username pode conter apenas letras, números, ponto, underline e #");
            }
            if (userRepository.existsByUsername(username)) {
                throw new ConflictException("Username já está em uso");
            }
            user.setUsername(username);
        }
        return buildProfile(user);
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        User user = findOrThrow(userId);
        // FK cascades (V4) remove friendships, memberships, sessions, telemetry,
        // results, achievements, push tokens and groups the user created.
        userRepository.delete(user);
    }

    public UserResponse getUserById(UUID targetId, UUID currentUserId) {
        User target = findOrThrow(targetId);
        if (!targetId.equals(currentUserId)) {
            boolean areFriends = friendshipRepository
                .findBetween(currentUserId, targetId)
                .map(f -> f.getStatus() == Friendship.Status.accepted)
                .orElse(false);
            boolean haveGroupInCommon = groupMemberRepository.haveGroupInCommon(currentUserId, targetId);
            if (!areFriends && !haveGroupInCommon) {
                throw new ForbiddenException("Perfil privado");
            }
        }
        return buildProfile(target);
    }

    public List<WeeklyStatsEntry> getWeeklyStats(UUID userId) {
        List<WeeklyStatsEntry> result = new ArrayList<>();
        LocalDate today = LocalDate.now(BRAZIL_ZONE);
        LocalDate currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (int i = 0; i < 8; i++) {
            LocalDate weekStart = currentWeekStart.minusWeeks(i);
            LocalDate weekEnd = weekStart.plusWeeks(1);
            Instant from = weekStart.atStartOfDay(BRAZIL_ZONE).toInstant();
            Instant to = weekEnd.atStartOfDay(BRAZIL_ZONE).toInstant();

            List<com.runpack.api.entity.RunResult> runs =
                runResultRepository.findByUserIdAndFinishedAtBetween(userId, from, to);

            double totalDistance = runs.stream().mapToDouble(r -> r.getTotalDistanceM()).sum();
            result.add(new WeeklyStatsEntry(weekStart, totalDistance, runs.size()));
        }
        return result;
    }

    public List<UserSearchResult> search(String query, UUID currentUserId) {
        if (query == null || query.trim().length() < 2) {
            throw new BadRequestException("Query deve ter no mínimo 2 caracteres");
        }
        List<User> users = userRepository.searchByUsername(query.trim(), currentUserId);
        return users.stream().map(u -> {
            Optional<Friendship> friendship = friendshipRepository.findBetween(currentUserId, u.getId());
            String relation = "none";
            UUID friendshipId = null;
            boolean favorite = false;
            if (friendship.isPresent()) {
                Friendship f = friendship.get();
                friendshipId = f.getId();
                relation = switch (f.getStatus()) {
                    case accepted -> "accepted";
                    case pending -> f.getRequester().getId().equals(currentUserId)
                        ? "pending_sent" : "pending_received";
                    default -> "none";
                };
                favorite = f.getRequester().getId().equals(currentUserId)
                    ? f.isRequesterFavorite()
                    : f.isAddresseeFavorite();
            }
            return new UserSearchResult(
                u.getId(), u.getName(), u.getUsername(), u.getAvatarUrl(), friendshipId, relation, favorite);
        }).toList();
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    private UserResponse buildProfile(User user) {
        long totalRuns = runResultRepository.countByUserId(user.getId());
        double totalDistanceM = runResultRepository.sumTotalDistanceByUserId(user.getId());
        double bestPaceSkm = runResultRepository.findBestPaceByUserId(user.getId()).orElse(0.0);
        return toResponse(user, (int) totalRuns, totalDistanceM, bestPaceSkm);
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getUsername(),
            user.getEmail(), user.getAvatarUrl(), 0, 0.0, 0.0, user.getCreatedAt());
    }

    private static UserResponse toResponse(User user, int totalRuns, double totalDistanceM, double bestPaceSkm) {
        return new UserResponse(user.getId(), user.getName(), user.getUsername(),
            user.getEmail(), user.getAvatarUrl(), totalRuns, totalDistanceM, bestPaceSkm, user.getCreatedAt());
    }
}
