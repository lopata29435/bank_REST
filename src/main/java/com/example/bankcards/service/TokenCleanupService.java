package com.example.bankcards.service;

import com.example.bankcards.security.RefreshTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final RefreshTokenProvider refreshTokenProvider;

    @Value("${jwt.cleanup.interval}")
    private long cleanupIntervalMs;

    @Async
    @Scheduled(fixedRateString = "${jwt.cleanup.interval}")
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired and revoked refresh tokens (interval: {}ms)", cleanupIntervalMs);

        try {
            long startTime = System.currentTimeMillis();
            int deletedCount = refreshTokenProvider.cleanupExpiredTokens();
            long duration = System.currentTimeMillis() - startTime;

            if (deletedCount > 0) {
                log.info("Successfully cleaned up {} expired refresh tokens in {}ms", deletedCount, duration);
            } else {
                log.debug("No expired tokens found for cleanup, completed in {}ms", duration);
            }
        } catch (Exception e) {
            log.error("Error during token cleanup: {}", e.getMessage(), e);
        }
    }
}
