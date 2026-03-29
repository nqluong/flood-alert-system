package org.project.floodalert.floodprocessor.repository;

import org.project.floodalert.floodprocessor.model.EventContributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventContributorRepository extends JpaRepository<EventContributor, UUID> {

    List<EventContributor> findByFloodEventIdOrderByCreatedAtDesc(UUID floodEventId);

    Optional<EventContributor> findByFloodEventIdAndUserId(UUID floodEventId, UUID userId);

    @Query("SELECT COUNT(e) FROM EventContributor e WHERE e.floodEventId = :floodEventId")
    Long countByFloodEventId(@Param("floodEventId") UUID floodEventId);
}
