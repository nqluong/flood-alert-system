package org.project.floodalert.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextUtils {
    /**
     * Lấy userId của user hiện tại
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof InternalUserDetails) {
            return ((InternalUserDetails) authentication.getPrincipal()).getUserId();
        }
        return null;
    }

    /**
     * Lấy email của user hiện tại
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof InternalUserDetails) {
            return ((InternalUserDetails) authentication.getPrincipal()).getEmail();
        }
        return null;
    }

    /**
     * Lấy InternalUserDetails của user hiện tại
     */
    public static InternalUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof InternalUserDetails) {
            return (InternalUserDetails) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Kiểm tra user có role cụ thể không
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(roleWithPrefix));
    }

    /**
     * Kiểm tra user có ít nhất một trong các roles
     */
    public static boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
}
