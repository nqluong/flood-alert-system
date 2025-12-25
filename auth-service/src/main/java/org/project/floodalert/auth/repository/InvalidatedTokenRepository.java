package org.project.floodalert.auth.repository;

import org.project.floodalert.auth.model.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, UUID> {

    boolean existsByTokenJti(String tokenJti);


    @Query("SELECT CASE WHEN COUNT(it) > 0 THEN true ELSE false END " +
            "FROM InvalidatedToken it " +
            "WHERE it.tokenJti = :tokenJti " +
            "AND it.expiresAt > :now")
    boolean isTokenInvalidated(@Param("tokenJti") String tokenJti,
                               @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM InvalidatedToken it WHERE it.expiresAt < :cutoffTime")
    int deleteExpiredTokens(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Modifying
    @Query("DELETE FROM InvalidatedToken it WHERE it.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(it) FROM InvalidatedToken it " +
            "WHERE it.userId = :userId AND it.expiresAt > :now")
    long countActiveInvalidatedTokensByUserId(@Param("userId") UUID userId,
                                              @Param("now") LocalDateTime now);

}
