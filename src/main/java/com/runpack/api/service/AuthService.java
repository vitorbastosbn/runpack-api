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

import java.security.SecureRandom;
import java.text.Normalizer;

@Service
@Transactional
public class AuthService {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

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
            String name = claims.name() != null ? claims.name() : claims.email();
            user.setName(name);
            user.setAvatarUrl(claims.pictureUrl());
            user.setProvider(provider);
            user.setProviderId(claims.providerId());
            user.setUsername(generateUniqueUsername(name));
            user = userRepository.save(user);
            isNewUser = true;
        } else {
            if (user.getProviderId() == null || !user.getProviderId().equals(claims.providerId())) {
                user.setProviderId(claims.providerId());
                user.setProvider(provider);
            }
        }

        String jwt = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getUsername());

        return new AuthResponse(jwt, isNewUser, user.getId().toString(), user.getEmail(), user.getUsername());
    }

    private String generateUniqueUsername(String name) {
        String slug = slugify(name);
        if (slug.isEmpty()) slug = "user";

        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = slug + "#" + randomSuffix(4);
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }
        // fallback: longer suffix to virtually guarantee uniqueness
        return slug + "#" + randomSuffix(8);
    }

    private String slugify(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        return normalized
                .replaceAll("\\p{M}", "")       // remove combining marks (accents)
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");    // keep only lowercase alphanumeric
    }

    private String randomSuffix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}
