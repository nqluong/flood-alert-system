package org.project.floodalert.floodcore.repository;

import org.project.floodalert.floodcore.model.CoreActiveFlood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoreActiveFloodRepository extends JpaRepository<CoreActiveFlood,String> {
}
