package org.project.floodalert.auth.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.model.AuditLog;
import org.project.floodalert.auth.repository.AuditLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogger {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log user login action
     */
    @Async
    public void logLogin(UUID userId, String ipAddress, String userAgent) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("action_type", "LOGIN");
        metadata.put("auth_method", "JWT");

        saveAuditLog(userId, "LOGIN", "USER", userId, ipAddress, userAgent, metadata);
    }

    /**
     * Log user logout action
     */
    @Async
    public void logLogout(UUID userId, String ipAddress, String userAgent) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("action_type", "LOGOUT");

        saveAuditLog(userId, "LOGOUT", "USER", userId, ipAddress, userAgent, metadata);
    }

    /**
     * Log user registration
     */
    @Async
    public void logUserRegistration(UUID userId, String ipAddress, String userAgent) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("action_type", "USER_REGISTRATION");
        metadata.put("auth_provider", "LOCAL");

        saveAuditLog(userId, "REGISTER", "USER", userId, ipAddress, userAgent, metadata);
    }

    /**
     * Log token refresh action
     */
    @Async
    public void logTokenRefresh(UUID userId, String ipAddress, String userAgent) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("action_type", "TOKEN_REFRESH");

        saveAuditLog(userId, "REFRESH_TOKEN", "USER", userId, ipAddress, userAgent, metadata);
    }

    /**
     * Log failed login attempt
     */
    @Async
    public void logFailedLogin(String email, String ipAddress, String userAgent, String reason) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("action_type", "FAILED_LOGIN");
        metadata.put("email", email);
        metadata.put("reason", reason);

        saveAuditLog(null, "FAILED_LOGIN", "USER", null, ipAddress, userAgent, metadata);
    }

    /**
     * Log password change
     */
    @Async
    public void logPasswordChange(UUID userId, String ipAddress, String userAgent) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("action_type", "PASSWORD_CHANGE");

        saveAuditLog(userId, "PASSWORD_CHANGE", "USER", userId, ipAddress, userAgent, metadata);
    }

    /**
     * Generic method to save audit log
     */
    private void saveAuditLog(UUID userId, String action, String resourceType,
                              UUID resourceId, String ipAddress, String userAgent,
                              Map<String, Object> metadata) {
        try {
            InetAddress inetAddress = parseIpAddress(ipAddress);

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .ipAddress(inetAddress)
                    .userAgent(truncateUserAgent(userAgent))
                    .metadata(metadata)
                    .build();

            auditLogRepository.save(auditLog);

            log.debug("Audit log saved: action={}, userId={}, ip={}", action, userId, ipAddress);

        } catch (Exception e) {
            // Don't throw exception to avoid breaking main business logic
            log.error("Failed to save audit log: action={}, userId={}", action, userId, e);
        }
    }

    private InetAddress parseIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return null;
        }

        try {
            // Handle IPv4 and IPv6
            return InetAddress.getByName(ipAddress.trim());
        } catch (UnknownHostException e) {
            log.warn("Invalid IP address for audit log: {}", ipAddress);
            return null;
        }
    }

    private String truncateUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }

        final int MAX_LENGTH = 500;
        return userAgent.length() > MAX_LENGTH
                ? userAgent.substring(0, MAX_LENGTH)
                : userAgent;
    }
}
