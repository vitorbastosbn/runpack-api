package com.runpack.api.service;

import com.runpack.api.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class OidcVerificationService {

    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    private final RestTemplate restTemplate;

    @Value("${runpack.oauth.skip-verification:false}")
    private boolean skipVerification;

    public OidcVerificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public OidcClaims verify(String provider, String idToken) {
        if (!"google".equals(provider)) {
            throw new UnauthorizedException("Provider não suportado: " + provider);
        }
        if (skipVerification) {
            return parseFallbackClaims(idToken);
        }
        return verifyGoogle(idToken);
    }

    @SuppressWarnings("unchecked")
    private OidcClaims verifyGoogle(String idToken) {
        String url = UriComponentsBuilder.fromHttpUrl(GOOGLE_TOKENINFO_URL)
                .queryParam("id_token", idToken)
                .toUriString();
        try {
            Map<String, String> claims = restTemplate.getForObject(url, Map.class);
            if (claims == null) {
                throw new UnauthorizedException("Token de autenticação inválido");
            }
            String issuer = claims.get("iss");
            if (!"accounts.google.com".equals(issuer) && !"https://accounts.google.com".equals(issuer)) {
                throw new UnauthorizedException("Token de autenticação inválido");
            }
            String expStr = claims.get("exp");
            if (expStr != null && Long.parseLong(expStr) < System.currentTimeMillis() / 1000) {
                throw new UnauthorizedException("Token de autenticação expirado");
            }
            return new OidcClaims(
                    claims.get("sub"),
                    claims.get("email"),
                    claims.getOrDefault("name", claims.getOrDefault("email", "Usuário")),
                    claims.get("picture")
            );
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Token de autenticação inválido");
        }
    }

    // Dev fallback: parse JWT payload without signature verification
    private OidcClaims parseFallbackClaims(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                return new OidcClaims("dev-user", "dev@runpack.app", "Dev User", null);
            }
            byte[] payloadBytes = java.util.Base64.getUrlDecoder().decode(parts[1] + "==");
            String payload = new String(payloadBytes);
            // Simple extraction without full JSON parsing for dev use
            String sub = extractJsonField(payload, "sub");
            String email = extractJsonField(payload, "email");
            String name = extractJsonField(payload, "name");
            String picture = extractJsonField(payload, "picture");
            return new OidcClaims(
                    sub != null ? sub : "dev-user",
                    email != null ? email : "dev@runpack.app",
                    name != null ? name : "Dev User",
                    picture
            );
        } catch (Exception e) {
            return new OidcClaims("dev-user", "dev@runpack.app", "Dev User", null);
        }
    }

    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = json.indexOf('"', start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    public record OidcClaims(
            String providerId,
            String email,
            String name,
            String pictureUrl
    ) {}
}
