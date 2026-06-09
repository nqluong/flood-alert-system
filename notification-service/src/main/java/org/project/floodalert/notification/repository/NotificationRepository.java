package org.project.floodalert.notification.repository;

import org.project.floodalert.notification.enums.NotificationStatus;
import org.project.floodalert.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // ==================== Mobile API Queries ====================

    /**
     * Lấy danh sách thông báo của user với pagination
     */
    @Query("""
        SELECT n FROM Notification n
        WHERE n.userId = :userId
          AND n.status IN ('SENT', 'DELIVERED', 'CLICKED')
        ORDER BY n.createdAt DESC
        """)
    Page<Notification> findByUserIdOrderByCreatedAtDesc(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    /**
     * Đếm số thông báo chưa đọc (chưa click)
     */
    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.userId = :userId
          AND n.status IN ('SENT', 'DELIVERED')
          AND n.clickedAt IS NULL
        """)
    long countUnreadByUserId(@Param("userId") UUID userId);

    /**
     * Đánh dấu thông báo đã đọc
     */
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.clickedAt = :clickedAt,
            n.status = 'CLICKED'
        WHERE n.id = :notificationId
          AND n.userId = :userId
          AND n.clickedAt IS NULL
        """)
    int markAsRead(
            @Param("notificationId") UUID notificationId,
            @Param("userId") UUID userId,
            @Param("clickedAt") LocalDateTime clickedAt
    );

    /**
     * Đánh dấu tất cả thông báo đã đọc
     */
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.clickedAt = :clickedAt,
            n.status = 'CLICKED'
        WHERE n.userId = :userId
          AND n.clickedAt IS NULL
          AND n.status IN ('SENT', 'DELIVERED')
        """)
    int markAllAsRead(
            @Param("userId") UUID userId,
            @Param("clickedAt") LocalDateTime clickedAt
    );

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.status = :status AND n.createdAt < :before")
    int deleteByStatusAndCreatedAtBefore(
            @Param("status") NotificationStatus status,
            @Param("before") LocalDateTime before
    );
}