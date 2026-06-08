package com.runpack.api.controller;

import com.runpack.api.dto.request.PushTokenRequest;
import com.runpack.api.dto.request.UpdateUserRequest;
import com.runpack.api.dto.response.UserResponse;
import com.runpack.api.dto.response.UserSearchResult;
import com.runpack.api.dto.response.WeeklyStatsEntry;
import com.runpack.api.entity.PushToken;
import com.runpack.api.entity.User;
import com.runpack.api.repository.PushTokenRepository;
import com.runpack.api.repository.UserRepository;
import com.runpack.api.security.CurrentUser;
import com.runpack.api.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PushTokenRepository pushTokenRepository;

    public UserController(UserService userService,
                          UserRepository userRepository,
                          PushTokenRepository pushTokenRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.pushTokenRepository = pushTokenRepository;
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

    @GetMapping("/users/me/stats")
    public ResponseEntity<List<WeeklyStatsEntry>> getMyStats(@CurrentUser UUID userId) {
        return ResponseEntity.ok(userService.getWeeklyStats(userId));
    }

    @PostMapping("/users/me/push-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void savePushToken(@CurrentUser UUID userId,
                               @RequestBody PushTokenRequest request) {
        User user = userRepository.getReferenceById(userId);
        PushToken token = pushTokenRepository
            .findByUserIdAndPlatform(userId, request.platform())
            .orElseGet(() -> {
                PushToken t = new PushToken();
                t.setUser(user);
                t.setPlatform(request.platform());
                return t;
            });
        token.setToken(request.token());
        pushTokenRepository.save(token);
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
