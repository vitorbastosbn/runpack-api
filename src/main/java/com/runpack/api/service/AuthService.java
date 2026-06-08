package com.runpack.api.service;

import com.runpack.api.dto.request.SocialLoginRequest;
import com.runpack.api.dto.response.AuthResponse;
import com.runpack.api.entity.User;
import com.runpack.api.exception.BadRequestException;
import com.runpack.api.exception.UnauthorizedException;
import com.runpack.api.repository.UserRepository;
import com.runpack.api.security.JwtTokenProvider;
import com.runpack.api.service.OidcVerificationService.OidcClaims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final OidcVerificationService oidcVerificationService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(OidcVerificationService oidcVerificationService,
                       UserRepository userRepository,
                       JwtTokenProvider jwtTokenProvider) {
        this.oidcVerificationService = oidcVerificationService;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse socialLogin(SocialLoginRequest request) {
        if (!"google".equals(request.provider())) {
            throw new BadRequestException("Provider não suportado: " + request.provider());
        }

        OidcClaims claims;
        try {
            claims = oidcVerificationService.verify(request.provider(), request.idToken());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Token de autenticação inválido");
        }

        User.Provider provider = User.Provider.google;
        boolean isNewUser = false;

        User user = userRepository
                .findByProviderAndProviderId(provider, claims.providerId())
                .orElseGet(() -> userRepository
                        .findByEmail(claims.email())
                        .orElse(null));

        if (user == null) {
            user = new User();
            user.setEmail(claims.email());
            user.setName(claims.name() != null ? claims.name() : claims.email());
            user.setAvatarUrl(claims.pictureUrl());
            user.setProvider(provider);
            user.setProviderId(claims.providerId());
            user = userRepository.save(user);
            isNewUser = true;
        } else {
            // Update provider link if user was found by email
            if (user.getProviderId() == null || !user.getProviderId().equals(claims.providerId())) {
                user.setProviderId(claims.providerId());
                user.setProvider(provider);
            }
        }

        String jwt = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getUsername());

        return new AuthResponse(jwt, isNewUser, user.getId().toString(), user.getEmail(), user.getUsername());
    }
}
