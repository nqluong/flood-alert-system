package org.project.floodalert.floodprocessor.repository;

import org.project.floodalert.floodprocessor.model.EventContributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventContributorRepository extends JpaRepository<EventContributor, UUID> {
}
