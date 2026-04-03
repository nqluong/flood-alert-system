package org.project.floodalert.notification.repository;

import org.project.floodalert.notification.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    List<NotificationPreference> findByEnabledTrue();

    List<NotificationPreference> findByUserIdIn(Collection<UUID> userIds);
}
