package org.project.floodalert.floodcore.repository.spec;

import org.project.floodalert.floodcore.dto.request.UserReportFilterRequest;
import org.project.floodalert.floodcore.model.UserReport;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class UserReportSpecification {

    private UserReportSpecification() {}

    /**
     * Xây dựng Specification tổng hợp từ userId và bộ filter.
     *
     * @param userId null → lấy tất cả người dùng; non-null → chỉ lấy của người dùng đó
     * @param filter chứa status, severityLevel
     */
    public static Specification<UserReport> build(UUID userId, UserReportFilterRequest filter) {
        return Specification
                .where(hasUserId(userId))
                .and(hasStatus(filter.getStatus()))
                .and(hasSeverityLevel(filter.getSeverityLevel()));
    }

    private static Specification<UserReport> hasUserId(UUID userId) {
        return (root, query, cb) ->
                userId == null ? null : cb.equal(root.get("userId"), userId);
    }

    private static Specification<UserReport> hasStatus(String status) {
        return (root, query, cb) ->
                (status == null || status.isBlank()) ? null : cb.equal(root.get("status"), status);
    }

    private static Specification<UserReport> hasSeverityLevel(String severityLevel) {
        return (root, query, cb) ->
                (severityLevel == null || severityLevel.isBlank()) ? null
                        : cb.equal(root.get("severityLevel"), severityLevel);
    }
}
