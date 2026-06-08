package com.runpack.api.controller;

import com.runpack.api.dto.request.SendFriendshipRequest;
import com.runpack.api.dto.request.UpdateFriendshipRequest;
import com.runpack.api.dto.response.FriendshipResponse;
import com.runpack.api.security.CurrentUser;
import com.runpack.api.service.FriendshipService;
import jakarta.validation.Valid;
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
    public ResponseEntity<List<FriendshipResponse>> getFriends(@CurrentUser UUID userId) {
        return ResponseEntity.ok(friendshipService.getFriends(userId));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<FriendshipResponse>> getPendingRequests(@CurrentUser UUID userId) {
        return ResponseEntity.ok(friendshipService.getPendingRequests(userId));
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @CurrentUser UUID userId) {
        friendshipService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
