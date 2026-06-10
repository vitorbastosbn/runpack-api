package com.runpack.api.controller;

import com.runpack.api.dto.request.SendFriendshipRequest;
import com.runpack.api.dto.request.UpdateFriendFavoriteRequest;
import com.runpack.api.dto.request.UpdateFriendshipRequest;
import com.runpack.api.dto.response.FriendshipResponse;
import com.runpack.api.security.CurrentUser;
import com.runpack.api.service.FriendshipService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/friendships")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @GetMapping
    public ResponseEntity<Page<FriendshipResponse>> getFriends(
            @CurrentUser UUID userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(friendshipService.getFriends(userId, pageable));
    }

    @GetMapping("/requests")
    public ResponseEntity<Page<FriendshipResponse>> getPendingRequests(
            @CurrentUser UUID userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(friendshipService.getPendingRequests(userId, pageable));
    }

    @GetMapping("/sent")
    public ResponseEntity<List<FriendshipResponse>> getSentRequests(@CurrentUser UUID userId) {
        return ResponseEntity.ok(friendshipService.getSentRequests(userId));
    }

    @PostMapping
    public ResponseEntity<FriendshipResponse> sendRequest(@CurrentUser UUID userId,
                                                           @Valid @RequestBody SendFriendshipRequest request) {
        return ResponseEntity.ok(friendshipService.sendRequest(userId, request.addresseeId()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FriendshipResponse> updateStatus(@PathVariable UUID id,
                                                            @CurrentUser UUID userId,
                                                            @Valid @RequestBody UpdateFriendshipRequest request) {
        return ResponseEntity.ok(friendshipService.updateStatus(id, userId, request.status()));
    }

    @PatchMapping("/{id}/favorite")
    public ResponseEntity<FriendshipResponse> updateFavorite(@PathVariable UUID id,
                                                              @CurrentUser UUID userId,
                                                              @Valid @RequestBody UpdateFriendFavoriteRequest request) {
        return ResponseEntity.ok(friendshipService.updateFavorite(id, userId, request.favorite()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @CurrentUser UUID userId) {
        friendshipService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
