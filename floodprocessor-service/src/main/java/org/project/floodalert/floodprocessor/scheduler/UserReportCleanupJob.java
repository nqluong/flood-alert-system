package org.project.floodalert.floodprocessor.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.service.lifecycle.UserReportProcessorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserReportCleanupJob {

    private final UserReportProcessorService userReportProcessorService;

    @Scheduled(fixedDelay = 1800000)
    public void cleanupPendingReports() {
        log.info("=== Bắt đầu Job: Dọn dẹp báo cáo PENDING ===");
        
        try {
            userReportProcessorService.cleanupPendingReports();
            log.info("=== Hoàn thành Job: Dọn dẹp báo cáo PENDING ===");
        } catch (Exception e) {
            log.error("Lỗi khi thực thi Job dọn dẹp báo cáo PENDING", e);
        }
    }

    @Scheduled(fixedDelay = 600000)
    public void applyTimeDecayAndSpatialCheck() {
        log.info("=== Bắt đầu Job: Lão hóa & Kiểm tra chéo không gian ===");
        
        try {
            userReportProcessorService.applyTimeDecayAndSpatialCheck();
            log.info("=== Hoàn thành Job: Lão hóa & Kiểm tra chéo không gian ===");
        } catch (Exception e) {
            log.error("Lỗi khi thực thi Job lão hóa và kiểm tra chéo", e);
        }
    }
}
