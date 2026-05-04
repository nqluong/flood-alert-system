package org.project.floodalert.ingestion.service;

import org.project.floodalert.ingestion.dto.SensorDataDTO;

/**
 * Service parse JSON payload thành SensorDataDTO
 */
public interface SensorDataParser {

    SensorDataDTO parse(String jsonPayload) throws Exception;
}
