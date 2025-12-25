package org.project.floodalert.auth.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.repository.InvalidatedTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final InvalidatedTokenRepository invalidatedTokenRepository;


    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired invalidated tokens");

        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
            int deletedCount = invalidatedTokenRepository.deleteExpiredTokens(cutoffTime);

            log.info("Cleanup completed: {} expired tokens deleted", deletedCount);
        } catch (Exception e) {
            log.error("Error during token cleanup", e);
        }
    }

    @Scheduled(cron = "0 0 */6 * * ?")
    @Transactional
    public void cleanupRecentlyExpiredTokens() {
        log.debug("Running quick cleanup of recently expired tokens");

        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
            int deletedCount = invalidatedTokenRepository.deleteExpiredTokens(cutoffTime);

            if (deletedCount > 0) {
                log.debug("Quick cleanup: {} tokens deleted", deletedCount);
            }
        } catch (Exception e) {
            log.error("Error during quick token cleanup", e);
        }
    }
}
