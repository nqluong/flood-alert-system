package org.project.floodalert.notification.repository;

import org.project.floodalert.notification.enums.NotificationStatus;
import org.project.floodalert.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserId(UUID userId);
    List<Notification> findByStatus(NotificationStatus status);

    @Query("""
        SELECT n FROM Notification n
        WHERE n.status = :status
        ORDER BY n.createdAt ASC
        LIMIT :limit
        """)
    List<Notification> findTopByStatusOrderByCreatedAtAsc(
            @Param("status") NotificationStatus status,
            @Param("limit") int limit
    );

    @Query("""
        SELECT n FROM Notification n
        WHERE n.status = :status
          AND n.retryCount < :maxRetries
          AND n.nextRetryAt <= :now
        ORDER BY n.nextRetryAt ASC
        """)
    List<Notification> findReadyForRetry(
            @Param("status") NotificationStatus status,
            @Param("maxRetries") int maxRetries,
            @Param("now") LocalDateTime now
    );


    @Query("""
        SELECT n FROM Notification n
        WHERE n.status = 'FAILED'
          AND n.retryCount < n.maxRetries
          AND n.nextRetryAt <= :now
        """)
    List<Notification> findPendingRetries(@Param("now") LocalDateTime now);
}