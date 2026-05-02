package org.project.floodalert.floodcore.repository;

import org.project.floodalert.floodcore.model.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, UUID>, JpaSpecificationExecutor<UserReport> {
    Optional<UserReport> findByReportId(String reportId);
}
