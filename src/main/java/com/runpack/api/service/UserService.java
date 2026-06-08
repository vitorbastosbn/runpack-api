package com.runpack.api.service;

import com.runpack.api.dto.request.UpdateUserRequest;
import com.runpack.api.dto.response.UserResponse;
import com.runpack.api.dto.response.UserSearchResult;
import com.runpack.api.entity.Friendship;
import com.runpack.api.entity.User;
import com.runpack.api.exception.BadRequestException;
import com.runpack.api.exception.ForbiddenException;
import com.runpack.api.exception.NotFoundException;
import com.runpack.api.repository.FriendshipRepository;
import com.runpack.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    public UserService(UserRepository userRepository, FriendshipRepository friendshipRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
    }

    public UserResponse getMe(UUID userId) {
        User user = findOrThrow(userId);
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateMe(UUID userId, UpdateUserRequest request) {
        User user = findOrThrow(userId);
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        return toResponse(user);
    }

    public UserResponse getUserById(UUID targetId, UUID currentUserId) {
        User target = findOrThrow(targetId);
        if (!targetId.equals(currentUserId)) {
            boolean areFriends = friendshipRepository
                    .findBetween(currentUserId, targetId)
                    .map(f -> f.getStatus() == Friendship.Status.accepted)
                    .orElse(false);
            if (!areFriends) throw new ForbiddenException("Perfil não disponível");
        }
        return toResponse(target);
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
            if (friendship.isPresent()) {
                Friendship f = friendship.get();
                friendshipId = f.getId();
                relation = switch (f.getStatus()) {
                    case accepted -> "accepted";
                    case pending -> f.getRequester().getId().equals(currentUserId)
                            ? "pending_sent" : "pending_received";
                    default -> "none";
                };
            }
            return new UserSearchResult(u.getId(), u.getName(), u.getUsername(), u.getAvatarUrl(), friendshipId, relation);
        }).toList();
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getUsername(), user.getEmail(), user.getAvatarUrl());
    }
}
