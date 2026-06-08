package com.runpack.api.controller;

import com.runpack.api.dto.request.SocialLoginRequest;
import com.runpack.api.dto.response.AuthResponse;
import com.runpack.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/social")
    public ResponseEntity<AuthResponse> social(@Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(authService.socialLogin(request));
    }
}
