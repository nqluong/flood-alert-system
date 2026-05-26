package org.project.floodalert.floodcore.repository.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.project.floodalert.floodcore.dto.request.UserReportFilterRequest;
import org.project.floodalert.floodcore.model.UserReport;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserReportSpecificationTest {

    private Root<UserReport> root;
    private CriteriaBuilder cb;
    private jakarta.persistence.criteria.CriteriaQuery<?> query;

    private Predicate predicate;
    private Path<Object> path;

    private UserReportFilterRequest filter;

    @BeforeEach
    void setUp() {
        root = mock(Root.class);
        cb = mock(CriteriaBuilder.class);
        query = mock(jakarta.persistence.criteria.CriteriaQuery.class);

        predicate = mock(Predicate.class);
        path = mock(Path.class);

        filter = new UserReportFilterRequest();

        when(root.get(anyString())).thenReturn(path);
        when(cb.equal(any(), any())).thenReturn(predicate);
    }

    @Test
    void build_allFiltersPresent_callsEqualForAllFields() {
        UUID userId = UUID.randomUUID();

        filter.setStatus("APPROVED");
        filter.setSeverityLevel("HIGH");

        Specification<UserReport> spec =
                UserReportSpecification.build(userId, filter);

        spec.toPredicate(root, query, cb);

        verify(root).get("userId");
        verify(cb).equal(path, userId);

        verify(root).get("status");
        verify(cb).equal(path, "APPROVED");

        verify(root).get("severityLevel");
        verify(cb).equal(path, "HIGH");
    }

    @Test
    void build_userIdNull_doesNotCallEqualForUserId() {
        filter.setStatus("APPROVED");
        filter.setSeverityLevel("HIGH");

        Specification<UserReport> spec =
                UserReportSpecification.build(null, filter);

        spec.toPredicate(root, query, cb);

        verify(root, never()).get("userId");
    }

    @Test
    void build_statusNull_doesNotCallEqualForStatus() {
        UUID userId = UUID.randomUUID();

        filter.setStatus(null);
        filter.setSeverityLevel("HIGH");

        Specification<UserReport> spec =
                UserReportSpecification.build(userId, filter);

        spec.toPredicate(root, query, cb);

        verify(root).get("userId");
        verify(root, never()).get("status");
        verify(root).get("severityLevel");
    }

    @Test
    void build_statusBlank_doesNotCallEqualForStatus() {
        UUID userId = UUID.randomUUID();

        filter.setStatus(" ");
        filter.setSeverityLevel("HIGH");

        Specification<UserReport> spec =
                UserReportSpecification.build(userId, filter);

        spec.toPredicate(root, query, cb);

        verify(root, never()).get("status");
    }

    @Test
    void build_severityLevelNull_doesNotCallEqualForSeverityLevel() {
        UUID userId = UUID.randomUUID();

        filter.setStatus("APPROVED");
        filter.setSeverityLevel(null);

        Specification<UserReport> spec =
                UserReportSpecification.build(userId, filter);

        spec.toPredicate(root, query, cb);

        verify(root).get("userId");
        verify(root).get("status");
        verify(root, never()).get("severityLevel");
    }

    @Test
    void build_severityLevelBlank_doesNotCallEqualForSeverityLevel() {
        UUID userId = UUID.randomUUID();

        filter.setStatus("APPROVED");
        filter.setSeverityLevel(" ");

        Specification<UserReport> spec =
                UserReportSpecification.build(userId, filter);

        spec.toPredicate(root, query, cb);

        verify(root, never()).get("severityLevel");
    }

    @Test
    void build_allFiltersNull_doesNotCallEqual() {
        Specification<UserReport> spec =
                UserReportSpecification.build(null, filter);

        spec.toPredicate(root, query, cb);

        verify(cb, never()).equal(any(), any());
    }

    @Test
    void build_onlyUserId_callsEqualOnce() {
        UUID userId = UUID.randomUUID();

        Specification<UserReport> spec =
                UserReportSpecification.build(userId, filter);

        spec.toPredicate(root, query, cb);

        verify(root).get("userId");
        verify(cb).equal(path, userId);

        verify(root, never()).get("status");
        verify(root, never()).get("severityLevel");
    }

    @Test
    void build_onlyStatus_callsEqualOnce() {
        filter.setStatus("PENDING");

        Specification<UserReport> spec =
                UserReportSpecification.build(null, filter);

        spec.toPredicate(root, query, cb);

        verify(root).get("status");
        verify(cb).equal(path, "PENDING");

        verify(root, never()).get("userId");
        verify(root, never()).get("severityLevel");
    }

    @Test
    void build_onlySeverityLevel_callsEqualOnce() {
        filter.setSeverityLevel("CRITICAL");

        Specification<UserReport> spec =
                UserReportSpecification.build(null, filter);

        spec.toPredicate(root, query, cb);

        verify(root).get("severityLevel");
        verify(cb).equal(path, "CRITICAL");

        verify(root, never()).get("userId");
        verify(root, never()).get("status");
    }
}