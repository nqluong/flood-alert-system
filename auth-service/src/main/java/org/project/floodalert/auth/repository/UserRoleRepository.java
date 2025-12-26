package org.project.floodalert.auth.repository;

import org.project.floodalert.auth.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    @Query("SELECT r.name FROM UserRole ur " +
            "JOIN Role r ON ur.roleId = r.id " +
            "WHERE ur.userId = :userId")
    List<String> findRoleNamesByUserId(@Param("userId") UUID userId);

    // Kiểm tra user đã có role chưa
    @Query(value = "SELECT EXISTS(" +
            "SELECT 1 FROM auth.user_roles ur " +
            "JOIN auth.roles r ON ur.role_id = r.id " +
            "WHERE ur.user_id = :userId AND r.name = :roleName)",
            nativeQuery = true)
    boolean hasRole(@Param("userId") UUID userId, @Param("roleName") String roleName);

    List<UserRole> findByUserId(UUID userId);

    List<UserRole> findByRoleId(UUID roleId);

    Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId);

    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);

    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId")
    List<UserRole> findAllRolesForUser(@Param("userId") UUID userId);
}


