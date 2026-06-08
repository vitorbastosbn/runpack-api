package com.runpack.api.controller;

import com.runpack.api.dto.request.UpdateUserRequest;
import com.runpack.api.dto.response.UserResponse;
import com.runpack.api.dto.response.UserSearchResult;
import com.runpack.api.security.CurrentUser;
import com.runpack.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/me")
    public ResponseEntity<UserResponse> getMe(@CurrentUser UUID userId) {
        return ResponseEntity.ok(userService.getMe(userId));
    }

    @PatchMapping("/users/me")
    public ResponseEntity<UserResponse> updateMe(@CurrentUser UUID userId,
                                                  @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateMe(userId, request));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id,
                                                 @CurrentUser UUID currentUserId) {
        return ResponseEntity.ok(userService.getUserById(id, currentUserId));
    }

    @GetMapping("/users/search")
    public ResponseEntity<List<UserSearchResult>> search(@RequestParam String q,
                                                          @CurrentUser UUID currentUserId) {
        return ResponseEntity.ok(userService.search(q, currentUserId));
    }
}
