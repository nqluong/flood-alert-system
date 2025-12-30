package org.project.floodalert.common.security.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.common.exception.ErrorCode;
import org.project.floodalert.common.security.CustomAccessDeniedHandler;
import org.project.floodalert.common.security.InternalUserDetails;
import org.project.floodalert.common.security.annotation.RequireOwnershipOrAdmin;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.UUID;

@Slf4j
@Aspect
@Component
public class OwnershipCheckAspect {
    @Before("@annotation(org.project.floodalert.common.security.annotation.RequireOwnershipOrAdmin)")
    public void checkOwnership(JoinPoint joinPoint) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Không có quyền truy cập");
        }

        InternalUserDetails userDetails = (InternalUserDetails) authentication.getPrincipal();
        String currentUserId = userDetails.getUserId();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireOwnershipOrAdmin annotation = method.getAnnotation(RequireOwnershipOrAdmin.class);

        // Kiểm tra quyền admin nếu được phép
        if (annotation.allowAdmin() && hasAdminRole(authentication)) {
            log.debug("Admin access granted for user: {}", currentUserId);
            return;
        }

        // Lấy userId từ parameter được chỉ định
        String targetUserId = extractUserIdFromParameters(
                joinPoint,
                signature,
                annotation.userIdParam()
        );

        // Kiểm tra ownership
        if (targetUserId == null || !targetUserId.equals(currentUserId)) {
            log.warn("Access denied. Current user: {}, Target user: {}", currentUserId, targetUserId);
            throw new AppException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền truy cập tài nguyên này");
        }

        log.debug("Ownership verified for user: {}", currentUserId);
    }

    private boolean hasAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }

    private String extractUserIdFromParameters(JoinPoint joinPoint,
                                               MethodSignature signature,
                                               String paramName) {
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = signature.getMethod().getParameters();

        // Tìm parameter theo tên
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(paramName)) {
                Object arg = args[i];
                if (arg instanceof UUID) {
                    return arg.toString();
                } else if (arg instanceof String) {
                    return (String) arg;
                }
            }
        }

        // Nếu không tìm thấy, thử extract từ Authentication object
        for (Object arg : args) {
            if (arg instanceof Authentication) {
                Authentication auth = (Authentication) arg;
                if (auth.getPrincipal() instanceof InternalUserDetails) {
                    InternalUserDetails details = (InternalUserDetails) auth.getPrincipal();
                    return details.getUserId();
                }
            }
        }

        return null;
    }
}
