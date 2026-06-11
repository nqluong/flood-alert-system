package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.PageResponse;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodcore.dto.event.UserReportEvent;
import org.project.floodalert.floodcore.dto.request.UserReportFilterRequest;
import org.project.floodalert.floodcore.dto.request.UserReportRequest;
import org.project.floodalert.floodcore.dto.response.UserReportResponse;
import org.project.floodalert.floodcore.exception.CoreErrorCode;
import org.project.floodalert.floodcore.kafka.UserReportEventProducer;
import org.project.floodalert.floodcore.mapper.UserReportMapper;
import org.project.floodalert.floodcore.model.UserReport;
import org.project.floodalert.floodcore.repository.UserReportRepository;
import org.project.floodalert.floodcore.repository.spec.UserReportSpecification;
import org.project.floodalert.floodcore.service.UserReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserReportServiceImpl implements UserReportService {

    private final UserReportRepository userReportRepository;
    private final UserReportMapper userReportMapper;
    private final UserReportEventProducer userReportEventProducer;

    @Override
    @Transactional
    public UserReportResponse submitUserReport(UserReportRequest request, UUID userId) {
        try {
            validateCoordinates(request);

            // Sinh mã báo cáo duy nhất theo format REPORT-yyyyMMddHHmmss-<4 ký tự ngẫu nhiên>
            String reportId = generateReportId();

            // Chuyển request thành entity và lưu vào database
            UserReport report = userReportMapper.toEntity(request, userId, reportId);
            UserReport savedReport = userReportRepository.save(report);
            log.info("[UserReportService] Lưu báo cáo thành công - reportId={}, userId={}", reportId, userId);

            UserReportEvent event = userReportMapper.toEvent(savedReport, request.getAddress());
            publishAfterCommit(event);

            // Trả về response cho client
            return userReportMapper.toResponse(savedReport);

        } catch (AppException e) {
            log.warn("[UserReportService] Lỗi nghiệp vụ khi xử lý báo cáo từ userId={}: {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[UserReportService] Lỗi hệ thống khi xử lý báo cáo từ userId={}: {}", userId, e.getMessage(), e);
            throw new AppException(CoreErrorCode.DATABASE_ERROR, "Không thể lưu báo cáo do lỗi hệ thống");
        }
    }

    private void publishAfterCommit(UserReportEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    userReportEventProducer.publish(event);
                }
            });
        } else {
            userReportEventProducer.publish(event);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserReportResponse> getReportsByUser(UUID userId, UserReportFilterRequest filter) {
        log.info("[UserReportService] Lấy báo cáo của userId={}, filter={}", userId, filter);

        Pageable pageable = buildPageable(filter);
        Specification<UserReport> spec = UserReportSpecification.build(userId, filter);
        Page<UserReport> page = userReportRepository.findAll(spec, pageable);

        List<UserReportResponse> content = page.getContent().stream()
                .map(userReportMapper::toSummaryResponse)
                .collect(Collectors.toList());

        return PageResponse.of(content, filter.getPage(), filter.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserReportResponse> getAllReports(UserReportFilterRequest filter) {
        log.info("[UserReportService] Lấy tất cả báo cáo (admin), filter={}", filter);

        Pageable pageable = buildPageable(filter);
        Specification<UserReport> spec = UserReportSpecification.build(null, filter);
        Page<UserReport> page = userReportRepository.findAll(spec, pageable);

        List<UserReportResponse> content = page.getContent().stream()
                .map(userReportMapper::toSummaryResponse)
                .collect(Collectors.toList());

        return PageResponse.of(content, filter.getPage(), filter.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private Pageable buildPageable(UserReportFilterRequest filter) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(filter.getSortDirection())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(filter.getPage(), filter.getSize(),
                Sort.by(direction, filter.getSortBy()));
    }

    private void validateCoordinates(UserReportRequest request) {
        if (request.getLat() == null || request.getLon() == null) {
            throw new AppException(CoreErrorCode.INVALID_COORDINATES, "Vĩ độ và kinh độ không được để trống");
        }
        if (request.getLat() < -90 || request.getLat() > 90
                || request.getLon() < -180 || request.getLon() > 180) {
            throw new AppException(CoreErrorCode.INVALID_COORDINATES,
                    "Vĩ độ phải nằm trong [-90, 90] và kinh độ nằm trong [-180, 180]");
        }
    }

    private String generateReportId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "REPORT-" + timestamp + "-" + suffix;
    }
}
