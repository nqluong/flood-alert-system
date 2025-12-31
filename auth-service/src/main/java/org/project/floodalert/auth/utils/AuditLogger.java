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
     * Log successful login action
     */
    @Async
    public void logLogin(UUID userId, String email, String ipAddress, String userAgent, String loginMethod) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("action_type", "LOGIN");
        metadata.put("status", "SUCCESS");
        metadata.put("email", email);
        metadata.put("login_method", loginMethod != null ? loginMethod : "EMAIL_PASSWORD");
        metadata.put("auth_method", "JWT");
        metadata.put("timestamp", LocalDateTime.now().toString());

        saveAuditLog(userId, "LOGIN", "AUTHENTICATION", userId, ipAddress, userAgent, metadata);
    }

    /**
     * Log user logout action
     */
    @Async
    public void logLogout(UUID userId, String email, String ipAddress, String userAgent) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("action_type", "LOGOUT");
        metadata.put("status", "SUCCESS");
        metadata.put("email", email);
        metadata.put("timestamp", LocalDateTime.now().toString());

        saveAuditLog(userId, "LOGOUT", "AUTHENTICATION", userId, ipAddress, userAgent, metadata);
    }

    /**
     * Log user registration
     */
    @Async
    public void logUserRegistration(UUID userId, String email, String ipAddress, String userAgent) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("action_type", "USER_REGISTRATION");
        metadata.put("status", "SUCCESS");
        metadata.put("auth_provider", "LOCAL");
        metadata.put("email", email);
        metadata.put("timestamp", LocalDateTime.now().toString());

        saveAuditLog(userId, "REGISTER", "USER", userId, ipAddress, userAgent, metadata);
    }

    /**
     * Log token refresh action
     */
    @Async
    public void logTokenRefresh(UUID userId, String email, String ipAddress, String userAgent, boolean success) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("action_type", "TOKEN_REFRESH");
        metadata.put("status", success ? "SUCCESS" : "FAILED");
        metadata.put("email", email);
        metadata.put("timestamp", LocalDateTime.now().toString());

        saveAuditLog(userId, "REFRESH_TOKEN", "AUTHENTICATION", userId, ipAddress, userAgent, metadata);
    }

    /**
     * Log failed login attempt
     */
    @Async
    public void logFailedLogin(String email, String ipAddress, String userAgent, String reason, String loginMethod) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("action_type", "LOGIN");
        metadata.put("status", "FAILED");
        metadata.put("email", email);
        metadata.put("reason", reason);
        metadata.put("login_method", loginMethod != null ? loginMethod : "EMAIL_PASSWORD");
        metadata.put("timestamp", LocalDateTime.now().toString());

        saveAuditLog(null, "FAILED_LOGIN", "AUTHENTICATION", null, ipAddress, userAgent, metadata);
    }

    /**
     * Log password change
     */
    @Async
    public void logPasswordChange(UUID userId, String email, String ipAddress, String userAgent, boolean success) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("action_type", "PASSWORD_CHANGE");
        metadata.put("status", success ? "SUCCESS" : "FAILED");
        metadata.put("email", email);
        metadata.put("timestamp", LocalDateTime.now().toString());

        saveAuditLog(userId, "PASSWORD_CHANGE", "USER", userId, ipAddress, userAgent, metadata);
    }

    @Async
    public void logSuspiciousActivity(UUID userId, String email, String ipAddress, String userAgent, String activity, String details) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("action_type", "SUSPICIOUS_ACTIVITY");
        metadata.put("activity", activity);
        metadata.put("details", details);
        metadata.put("email", email);
        metadata.put("timestamp", LocalDateTime.now().toString());

        saveAuditLog(userId, "SUSPICIOUS_ACTIVITY", "SECURITY", userId, ipAddress, userAgent, metadata);
    }

    @Async
    public void logAccountLockout(UUID userId, String email, String ipAddress, String userAgent, String reason) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("action_type", "ACCOUNT_LOCKOUT");
        metadata.put("status", "LOCKED");
        metadata.put("email", email);
        metadata.put("reason", reason);
        metadata.put("timestamp", LocalDateTime.now().toString());

        saveAuditLog(userId, "ACCOUNT_LOCKOUT", "SECURITY", userId, ipAddress, userAgent, metadata);
    }

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

            log.debug("Audit log saved: action={}, userId={}, ip={}, status={}",
                    action, userId, ipAddress, metadata.get("status"));

        } catch (Exception e) {
            log.error("Failed to save audit log: action={}, userId={}, metadata={}",
                    action, userId, metadata, e);
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
