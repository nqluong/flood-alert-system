package org.project.floodalert.auth.repository;

import org.project.floodalert.auth.model.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {

    @Modifying
    @Query("UPDATE UserAddress ua SET ua.isPrimary = false WHERE ua.userId = :userId AND ua.id != :excludeId")
    void unsetPrimaryForUser(@Param("userId") UUID userId, @Param("excludeId") UUID excludeId);

    @Modifying
    @Query("UPDATE UserAddress ua SET ua.isPrimary = false WHERE ua.userId = :userId")
    void unsetAllPrimaryForUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(ua) FROM UserAddress ua WHERE ua.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT ua FROM UserAddress ua WHERE ua.userId = :userId AND ua.isPrimary = true")
    Optional<UserAddress> findPrimaryByUserId(@Param("userId") UUID userId);

    @Query("SELECT ua FROM UserAddress ua WHERE ua.userId = :userId ORDER BY ua.isPrimary DESC, ua.createdAt DESC")
    List<UserAddress> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT ua FROM UserAddress ua WHERE ua.id = :id AND ua.userId = :userId")
    Optional<UserAddress> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

}
