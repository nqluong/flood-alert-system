package org.project.floodalert.auth.repository;

import org.project.floodalert.auth.model.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, UUID> {

    Optional<FcmToken> findByUserIdAndDeviceId(UUID userId, String deviceId);

    Optional<FcmToken> findByToken(String token);

    List<FcmToken> findByUserId(UUID userId);

    @Modifying
    @Query("UPDATE FcmToken f SET f.isActive = false WHERE f.userId = :userId")
    void deactivateAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE FcmToken f SET f.isActive = false WHERE f.userId = :userId AND f.deviceId = :deviceId")
    void deactivateByUserIdAndDeviceId(@Param("userId") UUID userId, @Param("deviceId") String deviceId);

    @Query("SELECT f FROM FcmToken f WHERE f.lastUsedAt < :threshold AND f.isActive = true")
    List<FcmToken> findStaleTokens(@Param("threshold") LocalDateTime threshold);
}
