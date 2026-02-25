package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.dto.response.AdminActiveFloodResponse;
import org.project.floodalert.floodcore.model.CoreActiveFlood;
import org.project.floodalert.floodcore.repository.CoreActiveFloodRepository;
import org.project.floodalert.floodcore.service.AdminFloodService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFloodServiceImpl implements AdminFloodService {
    private final CoreActiveFloodRepository coreActiveFloodRepository;

    @Transactional(readOnly = true)
    @Override
    public List<AdminActiveFloodResponse> getAllActiveFloods() {

        List<CoreActiveFlood> entities = coreActiveFloodRepository.findAll();

        return entities.stream()
                .map(this::mapToResponse)
                .toList();

    }

    private AdminActiveFloodResponse mapToResponse(CoreActiveFlood entity) {
        return AdminActiveFloodResponse.builder()
                .eventId(entity.getEventId())
                .lat(entity.getLat() != null ? entity.getLat().doubleValue() : null)
                .lon(entity.getLon() != null ? entity.getLon().doubleValue() : null)
                .location(entity.getLocationDescription())
                .waterLevel(entity.getWaterLevel() != null ? entity.getWaterLevel().doubleValue() : null)
                .severityLevel(entity.getSeverityLevel())
                .status(entity.getStatus())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
