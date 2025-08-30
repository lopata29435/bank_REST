package com.example.bankcards.security;

import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.DatabaseOperationException;
import com.example.bankcards.exception.RefreshTokenExpiredException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.RefreshTokenRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenProvider {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh.expiration}")
    private long refreshExpirationMs;

    @Value("${app.max-sessions-per-user:5}")
    private int maxSessionsPerUser;

    @Transactional
    public String createRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> UserNotFoundException.byUsername(username));

        enforceSessionLimit(user);

        String token = UUID.randomUUID().toString();
        String tokenHash = hashToken(token);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        try {
            refreshTokenRepository.save(refreshToken);
            log.info("Created new refresh token for user: {}, active sessions: {}",
                    username, getActiveSessionsCount(user));
        } catch (Exception e) {
            throw new DatabaseOperationException("Failed to create refresh token", e);
        }

        return token;
    }

    @Transactional
    public void enforceSessionLimit(User user) {
        Instant now = Instant.now();
        int activeSessionsCount = refreshTokenRepository.countActiveByUser(user, now);

        if (activeSessionsCount >= maxSessionsPerUser) {
            var activeTokens = refreshTokenRepository.findActiveByUser(user, now);
            Collections.reverse(activeTokens);

            int tokensToRevoke = activeSessionsCount - maxSessionsPerUser + 1;

            for (int i = 0; i < tokensToRevoke && i < activeTokens.size(); i++) {
                RefreshToken oldToken = activeTokens.get(i);
                oldToken.setRevoked(true);
                refreshTokenRepository.save(oldToken);
                log.info("Revoked old refresh token for user: {} due to session limit", user.getUsername());
            }

            log.info("Enforced session limit for user: {}, revoked {} old sessions",
                    user.getUsername(), tokensToRevoke);
        }
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        String tokenHash = hashToken(token);
        return refreshTokenRepository.findByTokenHashAndRevokedFalseWithUser(tokenHash);
    }

    @Transactional(noRollbackFor = RefreshTokenExpiredException.class)
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiresAt().isBefore(Instant.now())) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Refresh token {} has been revoked due to expiration", token.getTokenHash());

            throw new RefreshTokenExpiredException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    public void revokeToken(String token) {
        String tokenHash = hashToken(token);
        refreshTokenRepository.revokeByTokenHash(tokenHash);
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    public int getActiveSessionsCount(User user) {
        return refreshTokenRepository.countActiveByUser(user, Instant.now());
    }

    @Transactional
    public int cleanupExpiredTokens() {
        Instant cutoffTime = Instant.now();

        int deletedCount = refreshTokenRepository.countExpiredAndRevoked(cutoffTime);

        if (deletedCount > 0) {
            refreshTokenRepository.deleteExpiredAndRevoked(cutoffTime);
            log.debug("Deleted {} expired and revoked refresh tokens", deletedCount);
        } else {
            log.debug("No expired tokens found for cleanup");
        }

        return deletedCount;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
