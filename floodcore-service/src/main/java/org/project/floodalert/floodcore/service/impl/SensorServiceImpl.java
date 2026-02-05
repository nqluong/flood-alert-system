package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.PageResponse;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodcore.dto.request.CreateSensorRequest;
import org.project.floodalert.floodcore.dto.request.SensorFilterRequest;
import org.project.floodalert.floodcore.dto.response.CreateSensorResponse;
import org.project.floodalert.floodcore.dto.response.SensorDetailResponse;
import org.project.floodalert.floodcore.dto.response.SensorMapResponse;
import org.project.floodalert.floodcore.dto.response.SensorSummaryResponse;
import org.project.floodalert.floodcore.exception.CoreErrorCode;
import org.project.floodalert.floodcore.mapper.SensorMapper;
import org.project.floodalert.floodcore.mapper.SensorReadMapper;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.model.SensorLog;
import org.project.floodalert.floodcore.repository.SensorLogRepository;
import org.project.floodalert.floodcore.repository.SensorRepository;
import org.project.floodalert.floodcore.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorServiceImpl implements SensorService {

    private final SensorRepository sensorRepository;
    private final SensorLogRepository sensorLogRepository;
    private final SensorValidationService sensorValidationService;
    private final ApiKeyGeneratorService apiKeyGeneratorService;
    private final SensorLogService sensorLogService;
    private final SensorRedisCache sensorRedisCache;
    private final SensorMapper sensorMapper;
    private final SensorReadMapper readMapper;

    @Override
    @Transactional
    public CreateSensorResponse createSensor(CreateSensorRequest request, UUID performedBy) {
        try {
            // Validate
            sensorValidationService.validateCreateRequest(request);

            // Generate API Key
            String apiKey = apiKeyGeneratorService.generateUniqueApiKey();

            // Tạo entity và lưu vào DB
            Sensor sensor = sensorMapper.toEntity(request, apiKey);
            sensor.setCreatedBy(performedBy);
            Sensor savedSensor = sensorRepository.save(sensor);

            // Ghi audit log
            sensorLogService.logSensorCreated(savedSensor, performedBy);

            // Đồng bộ Redis
            sensorRedisCache.syncSensorToRedis(savedSensor);

            // Tạo response
            log.info("Tạo sensor {} thành công", savedSensor.getSensorId());
            return sensorMapper.toCreateResponse(savedSensor);

        } catch (AppException e) {
            log.error("Lỗi nghiệp vụ khi tạo sensor {}: {}",
                    request.getSensorId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi hệ thống khi tạo sensor {}: {}",
                    request.getSensorId(), e.getMessage(), e);
            throw new AppException(CoreErrorCode.DATABASE_ERROR,
                    "Không thể tạo cảm biến do lỗi hệ thống");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SensorSummaryResponse> getSensorList(SensorFilterRequest filter) {
        log.info("Lấy danh sách sensor với filter: page={}, size={}, status={}, search={}",
                filter.getPage(), filter.getSize(), filter.getStatus(), filter.getSearch());

        // Kiểm tra cache
        Optional<PageResponse<SensorSummaryResponse>> cachedResult = sensorRedisCache.getCachedSensorList(filter);
        if (cachedResult.isPresent()) {
            log.info("Trả về danh sách sensor từ cache");
            return cachedResult.get();
        }

        // Query từ database
        Pageable pageable = buildPageable(filter);
        Page<Sensor> sensorPage = querySensors(filter, pageable);

        // Map sang DTO
        List<SensorSummaryResponse> content = sensorPage.getContent().stream()
                .map(readMapper::toSummaryResponse)
                .collect(Collectors.toList());

        PageResponse<SensorSummaryResponse> response = PageResponse.of(
                content,
                filter.getPage(),
                filter.getSize(),
                sensorPage.getTotalElements(),
                sensorPage.getTotalPages()
        );

        // Cache kết quả
        sensorRedisCache.cacheSensorList(filter, response);

        log.info("Đã lấy {} sensors từ database", content.size());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public SensorDetailResponse getSensorDetail(UUID sensorId, boolean includeLogs) {
        log.info("Lấy chi tiết sensor: {}, includeLogs={}", sensorId, includeLogs);

        // Kiểm tra cache (chỉ cache khi không include logs)
        if (!includeLogs) {
            Optional<SensorDetailResponse> cachedResult = sensorRedisCache.getCachedSensorDetail(sensorId);
            if (cachedResult.isPresent()) {
                return cachedResult.get();
            }
        }

        // Query từ database
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new AppException(CoreErrorCode.SENSOR_NOT_FOUND,
                        "Không tìm thấy sensor với ID: " + sensorId));

        // Map sang DTO
        SensorDetailResponse response = readMapper.toDetailResponse(sensor);

        // Lấy logs nếu được yêu cầu
        if (includeLogs) {
            log.debug("Lấy lịch sử logs cho sensor: {}", sensorId);
            List<SensorLog> logs = sensorLogRepository.findTop10BySensorIdOrderByCreatedAtDesc(sensorId);
            response.setLogs(readMapper.toLogResponses(logs));
        }

        // Cache kết quả (chỉ cache khi không include logs)
        if (!includeLogs) {
            sensorRedisCache.cacheSensorDetail(sensorId, response);
        }

        log.info("Đã lấy chi tiết sensor: {}", sensor.getSensorId());
        return response;
    }

    @Override
    public SensorDetailResponse getSensorDetailBySensorId(String sensorId, boolean includeLogs) {

        Sensor sensor = sensorRepository.findBySensorId(sensorId)
                .orElseThrow(() -> new AppException(CoreErrorCode.SENSOR_NOT_FOUND,
                        "Không tìm thấy sensor với mã: " + sensorId));

        return getSensorDetail(sensor.getId(), includeLogs);
    }

    @Override
    public SensorMapResponse getSensorMapData() {

        // Kiểm tra cache
        Optional<SensorMapResponse> cachedResult = sensorRedisCache.getCachedMapData();
        if (cachedResult.isPresent()) {
            log.info("Cache Hit - Lấy map data từ cache");
            return cachedResult.get();
        }

        // Query từ database
        List<Sensor> sensors = sensorRepository.findAll();

        // Map sang GeoJSON
        SensorMapResponse response = readMapper.toMapResponse(sensors);

        // Cache kết quả
        sensorRedisCache.cacheMapData(response);

        return response;
    }

    @Override
    public SensorMapResponse getActiveSensorMapData() {
        List<Sensor> sensors = sensorRepository.findAllActiveSensors();
        SensorMapResponse response = readMapper.toMapResponse(sensors);

        return response;
    }

    private Pageable buildPageable(SensorFilterRequest filter) {
        Sort sort = Sort.by(
                filter.getSortDirection().equalsIgnoreCase("ASC")
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC,
                filter.getSortBy()
        );

        return PageRequest.of(filter.getPage(), filter.getSize(), sort);
    }

    private Page<Sensor> querySensors(SensorFilterRequest filter, Pageable pageable) {
        boolean hasStatus = filter.getStatus() != null && !filter.getStatus().isEmpty();
        boolean hasSearch = filter.getSearch() != null && !filter.getSearch().isEmpty();

        if (hasStatus && hasSearch) {
            // Filter cả status và search
            log.debug("Query với status={} và search={}", filter.getStatus(), filter.getSearch());
            return sensorRepository.findByStatusAndSearch(filter.getStatus(), filter.getSearch(), pageable);
        } else if (hasStatus) {
            // Chỉ filter theo status
            log.debug("Query với status={}", filter.getStatus());
            return sensorRepository.findByStatus(filter.getStatus(), pageable);
        } else if (hasSearch) {
            // Chỉ search
            log.debug("Query với search={}", filter.getSearch());
            return sensorRepository.searchByNameOrSensorId(filter.getSearch(), pageable);
        } else {
            // Lấy tất cả
            log.debug("Query tất cả sensors");
            return sensorRepository.findAll(pageable);
        }
    }
}
