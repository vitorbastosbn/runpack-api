package com.runpack.api.service;

import com.runpack.api.dto.response.FriendshipResponse;
import com.runpack.api.entity.Friendship;
import com.runpack.api.entity.User;
import com.runpack.api.exception.BadRequestException;
import com.runpack.api.exception.ConflictException;
import com.runpack.api.exception.ForbiddenException;
import com.runpack.api.exception.NotFoundException;
import com.runpack.api.repository.FriendshipRepository;
import com.runpack.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final PushNotificationService pushService;

    public FriendshipService(FriendshipRepository friendshipRepository,
                              UserRepository userRepository,
                              PushNotificationService pushService) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.pushService = pushService;
    }

    public List<FriendshipResponse> getFriends(UUID userId) {
        List<Friendship> asRequester = friendshipRepository.findByRequester_IdAndStatus(userId, Friendship.Status.accepted);
        List<Friendship> asAddressee = friendshipRepository.findByAddressee_IdAndStatus(userId, Friendship.Status.accepted);
        List<FriendshipResponse> result = new ArrayList<>();
        for (Friendship f : asRequester) result.add(toResponse(f, userId));
        for (Friendship f : asAddressee) result.add(toResponse(f, userId));
        return result;
    }

    public List<FriendshipResponse> getPendingRequests(UUID userId) {
        return friendshipRepository.findByAddressee_IdAndStatus(userId, Friendship.Status.pending)
            .stream().map(f -> toResponse(f, userId)).toList();
    }

    public List<FriendshipResponse> getSentRequests(UUID userId) {
        return friendshipRepository.findByRequester_IdAndStatus(userId, Friendship.Status.pending)
            .stream().map(f -> toResponse(f, userId)).toList();
    }

    @Transactional
    public FriendshipResponse sendRequest(UUID requesterId, UUID addresseeId) {
        if (requesterId.equals(addresseeId)) {
            throw new BadRequestException("Não é possível adicionar a si mesmo");
        }
        User requester = findUser(requesterId);
        User addressee = findUser(addresseeId);

        var existing = friendshipRepository.findBetween(requesterId, addresseeId);
        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == Friendship.Status.rejected) {
                f.setRequester(requester);
                f.setAddressee(addressee);
                f.setStatus(Friendship.Status.pending);
                pushService.notifyFriendRequest(addresseeId, requester.getName());
                return toResponse(f, requesterId);
            }
            throw new ConflictException("Solicitação já existe");
        }

        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(Friendship.Status.pending);
        friendship = friendshipRepository.save(friendship);

        pushService.notifyFriendRequest(addresseeId, requester.getName());
        return toResponse(friendship, requesterId);
    }

    @Transactional
    public FriendshipResponse updateStatus(UUID friendshipId, UUID userId, String statusStr) {
        Friendship friendship = findOrThrow(friendshipId);
        if (!friendship.getAddressee().getId().equals(userId)) {
            throw new ForbiddenException("Sem permissão para alterar esta solicitação");
        }

        Friendship.Status newStatus = switch (statusStr) {
            case "accepted" -> Friendship.Status.accepted;
            case "rejected" -> Friendship.Status.rejected;
            default -> throw new BadRequestException("Status inválido: " + statusStr);
        };

        friendship.setStatus(newStatus);

        if (newStatus == Friendship.Status.accepted) {
            pushService.notifyFriendAccepted(
                friendship.getRequester().getId(),
                friendship.getAddressee().getName()
            );
        }

        return toResponse(friendship, userId);
    }

    @Transactional
    public void delete(UUID friendshipId, UUID userId) {
        Friendship friendship = findOrThrow(friendshipId);
        boolean involved = friendship.getRequester().getId().equals(userId)
            || friendship.getAddressee().getId().equals(userId);
        if (!involved) throw new ForbiddenException("Sem permissão");
        friendshipRepository.delete(friendship);
    }

    private Friendship findOrThrow(UUID id) {
        return friendshipRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Amizade não encontrada"));
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    private FriendshipResponse toResponse(Friendship f, UUID currentUserId) {
        User other = f.getRequester().getId().equals(currentUserId) ? f.getAddressee() : f.getRequester();
        FriendshipResponse.FriendUser friendUser = new FriendshipResponse.FriendUser(
            other.getId(), other.getName(), other.getUsername(), other.getAvatarUrl());
        return new FriendshipResponse(f.getId(), friendUser, f.getStatus().name(), f.getCreatedAt());
    }
}
