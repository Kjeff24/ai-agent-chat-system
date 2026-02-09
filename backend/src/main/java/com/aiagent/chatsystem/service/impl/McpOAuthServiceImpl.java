package com.aiagent.chatsystem.service.impl;

import com.aiagent.chatsystem.config.AppMcpOAuthProperties;
import com.aiagent.chatsystem.model.McpOAuthToken;
import com.aiagent.chatsystem.service.McpOAuthProviderRegistry;
import com.aiagent.chatsystem.repository.McpOAuthTokenRepository;
import com.aiagent.chatsystem.service.McpOAuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class McpOAuthServiceImpl implements McpOAuthService {

    private static final Logger logger = LoggerFactory.getLogger(McpOAuthServiceImpl.class);
    private static final int STATE_EXPIRY_MINUTES = 10;

    private final AppMcpOAuthProperties oauthProperties;
    private final McpOAuthProviderRegistry providerRegistry;
    private final McpOAuthTokenRepository tokenRepository;
    private final String jwtSecret;
    private final RestTemplate restTemplate = new RestTemplate();

    public McpOAuthServiceImpl(
            AppMcpOAuthProperties oauthProperties,
            McpOAuthProviderRegistry providerRegistry,
            McpOAuthTokenRepository tokenRepository,
            @Value("${jwt.secret}") String jwtSecret) {
        this.oauthProperties = oauthProperties;
        this.providerRegistry = providerRegistry;
        this.tokenRepository = tokenRepository;
        this.jwtSecret = jwtSecret;
    }

    @Override
    public String buildAuthorizeUrl(String serverName, UUID userId, String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return null;
        }
        AppMcpOAuthProperties.OAuthProviderConfig provider = providerRegistry.getProvider(providerId);
        if (provider == null || provider.getAuthorizationUri() == null || provider.getClientId() == null) {
            return null;
        }
        String callbackUri = oauthProperties.getCallbackUri();
        if (callbackUri.isBlank()) {
            return null;
        }
        String state = buildState(userId, serverName, providerId);
        String scope = provider.getScopes() != null ? provider.getScopes().trim() : "";
        String url = provider.getAuthorizationUri()
                + (provider.getAuthorizationUri().contains("?") ? "&" : "?")
                + "response_type=code"
                + "&client_id=" + urlEncode(provider.getClientId())
                + "&redirect_uri=" + urlEncode(callbackUri)
                + "&state=" + urlEncode(state);
        if (!scope.isEmpty()) {
            url += "&scope=" + urlEncode(scope);
        }
        return url;
    }

    @Override
    @Transactional
    public UUID exchangeCodeAndStoreToken(String code, String state) {
        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            return null;
        }
        UUID userId;
        String serverName;
        String providerId;
        try {
            Claims claims = parseState(state);
            userId = UUID.fromString(claims.get("userId", String.class));
            serverName = claims.get("serverName", String.class);
            providerId = claims.get("providerId", String.class);
        } catch (Exception e) {
            logger.warn("Invalid OAuth state: {}", e.getMessage());
            return null;
        }
        if (providerId == null || providerId.isBlank()) {
            return null;
        }
        AppMcpOAuthProperties.OAuthProviderConfig provider = providerRegistry.getProvider(providerId);
        if (provider == null || provider.getTokenUri() == null) {
            return null;
        }
        String callbackUri = oauthProperties.getCallbackUri();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", callbackUri);
        body.add("client_id", provider.getClientId());
        body.add("client_secret", provider.getClientSecret() != null ? provider.getClientSecret() : "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                provider.getTokenUri(),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Log token endpoint response (do not log full tokens in production)
        logger.debug("Token endpoint response status: {}", response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null) {
            logger.debug("Token response keys: {}", responseBody.keySet());
            if (responseBody.containsKey("expires_in")) {
                logger.debug("Token expires_in: {}", responseBody.get("expires_in"));
            }
            if (responseBody.containsKey("token_type")) {
                logger.debug("Token type: {}", responseBody.get("token_type"));
            }
            if (responseBody.containsKey("access_token")) {
                String at = (String) responseBody.get("access_token");
                logger.debug("Access token present, length={}", at != null ? at.length() : 0);
            }
        }

        if (response.getStatusCode() != HttpStatus.OK || responseBody == null) {
            logger.warn("OAuth token exchange failed: {} body={}", response.getStatusCode(), responseBody);
            return null;
        }
        Map<String, Object> json = responseBody;
        String accessToken = (String) json.get("access_token");
        String refreshToken = (String) json.get("refresh_token");
        Number expiresIn = (Number) json.get("expires_in");
        // Debug only: do not enable DEBUG logging in production (tokens are sensitive)
        logger.debug("Access token: {}", accessToken);
        logger.debug("Refresh token: {}", refreshToken);
        if (accessToken == null || accessToken.isBlank()) {
            logger.warn("OAuth response missing access_token");
            return null;
        }
        Instant expiresAt = expiresIn != null && expiresIn.intValue() > 0
                ? Instant.now().plusSeconds(expiresIn.longValue())
                : null;
        saveToken(userId, serverName, accessToken, refreshToken, expiresAt);
        logger.info("OAuth token stored for user {} server {}", userId, serverName);
        return userId;
    }

    @Override
    public String getValidAccessToken(UUID userId, String serverName) {
        if (userId == null || serverName == null || serverName.isBlank()) {
            return null;
        }
        McpOAuthToken token = tokenRepository.findByUserIdAndServerName(userId, serverName).orElse(null);
        if (token == null) {
            return null;
        }
        if (token.isExpired() && token.getRefreshToken() != null && !token.getRefreshToken().isBlank()) {
            // Optional: refresh token here; for now we just return null if expired
            return null;
        }
        if (token.isExpired()) {
            return null;
        }
        return token.getAccessToken();
    }

    @Override
    public boolean hasToken(UUID userId, String serverName) {
        return userId != null && serverName != null && tokenRepository.existsByUserIdAndServerName(userId, serverName);
    }

    @Override
    @Transactional
    public void revokeToken(UUID userId, String serverName) {
        if (userId != null && serverName != null) {
            tokenRepository.deleteByUserIdAndServerName(userId, serverName);
            logger.debug("OAuth token revoked for user {} server {}", userId, serverName);
        }
    }

    private String buildState(UUID userId, String serverName, String providerId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        Instant exp = now.plus(STATE_EXPIRY_MINUTES, ChronoUnit.MINUTES);
        return Jwts.builder()
                .claim("userId", userId.toString())
                .claim("serverName", serverName)
                .claim("providerId", providerId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    private Claims parseState(String state) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(state).getPayload();
    }

    private void saveToken(UUID userId, String serverName, String accessToken, String refreshToken, Instant expiresAt) {
        McpOAuthToken token = tokenRepository.findByUserIdAndServerName(userId, serverName)
                .orElseGet(McpOAuthToken::new);
        token.setUserId(userId);
        token.setServerName(serverName);
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setExpiresAt(expiresAt);
        token.setUpdatedAt(Instant.now());
        tokenRepository.save(token);
    }

    private static String urlEncode(String s) {
        if (s == null) return "";
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
