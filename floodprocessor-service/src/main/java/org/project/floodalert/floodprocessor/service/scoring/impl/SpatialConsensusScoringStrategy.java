package org.project.floodalert.floodprocessor.service.scoring.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.repository.FloodEventRepository;
import org.project.floodalert.floodprocessor.service.scoring.ReportScoringStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Chấm điểm dựa trên sự đồng thuận không gian (Spatial Consensus).
 *
 * <p>Thuật toán 2 bước:</p>
 * <ol>
 *   <li><b>Bước 1 — Kiểm tra IoT:</b> Nếu có trạm cảm biến trong bán kính {@value #RADIUS_KM}km
 *       cập nhật trong {@value #TIME_WINDOW_MINUTES} phút qua:
 *       <ul>
 *         <li>Trạm báo ngập (WARNING/DANGER hoặc mực nước &gt; {@value #FLOOD_WATER_LEVEL_CM}cm) tới 100.0 (bảo chứng vàng)</li>
 *         <li>Trạm báo an toàn (SAFE hoặc mực nước = 0cm) tới 0.0 (phủ quyết từ máy)</li>
 *       </ul>
 *   </li>
 *   <li><b>Bước 2 — Đồng thuận đám đông:</b> Đếm số {@code USER_REPORT} active lân cận
 *       và cho điểm theo thang: 0->0.0 | 1->50.0 | 2->80.0 | ≥3->100.0</li>
 * </ol>
 *
 * <p>Trọng số tối đa: <b>20 điểm</b> trong tổng điểm scoring (engine scale lại).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpatialConsensusScoringStrategy implements ReportScoringStrategy {

    private static final double RADIUS_KM = 0.5;
    private static final int TIME_WINDOW_MINUTES = 30;
    private static final int FLOOD_WATER_LEVEL_CM = 10;

    private static final List<String> FLOODING_SEVERITIES = List.of("WARNING", "DANGER");
    private static final String SAFE_SEVERITY = "SAFE";
    private static final BigDecimal FLOOD_THRESHOLD = BigDecimal.valueOf(FLOOD_WATER_LEVEL_CM);

    private final FloodEventRepository floodEventRepository;


    @Override
    public double calculateScore(ReportMessage msg) {
        log.info("[SCORING][SPATIAL] Bắt đầu cho reportId={}, lat={}, lon={} — bán kính={}km, cửa sổ={}min",
                msg.getReportId(), msg.getLat(), msg.getLon(), RADIUS_KM, TIME_WINDOW_MINUTES);

        LocalDateTime since = LocalDateTime.now().minusMinutes(TIME_WINDOW_MINUTES);

        // B1: Tín hiệu IoT
        List<FloodEvent> nearbyIotEvents = floodEventRepository.findRecentIotEventsNearby(
                msg.getLat(), msg.getLon(), RADIUS_KM, since);

        if (!nearbyIotEvents.isEmpty()) {
            return evaluateIotSignal(msg.getReportId(), nearbyIotEvents, msg.getLat(), msg.getLon(), since);
        }

        log.info("[SCORING][SPATIAL] reportId={} — Không có trạm IoT lân cận, chuyển sang đám đông.",
                msg.getReportId());

        // B2: Đồng thuận đám đông
        return evaluateCrowdConsensus(msg.getReportId(), msg.getLat(), msg.getLon(), since);
    }

    @Override
    public boolean isApplicable(ReportMessage msg) {
        return true;
    }

    @Override
    public String getStrategyName() {
        return "SPATIAL_CONSENSUS";
    }


    /**
     * Bước 1: Đánh giá tín hiệu từ các trạm IoT lân cận.
     *
     * <p>Luồng quyết định:</p>
     * <ul>
     *   <li>Trạm báo ngập -> Bảo chứng vàng (100.0) - không cần hỏi thêm.</li>
     *   <li>Trạm báo không ngập -> Kiểm tra lại đám đông trước khi phủ quyết:
     *     <ul>
     *       <li>Đám đông < 3 -> Tin máy móc (0.0).</li>
     *       <li>Đám đông ≥ 3 -> Đám đông phế truất quyền phủ quyết, lấy điểm đám đông.</li>
     *     </ul>
     *   </li>
     * </ul>
     */
    private double evaluateIotSignal(String reportId, List<FloodEvent> iotEvents,
                                     double lat, double lon, LocalDateTime since) {
        log.info("[SCORING][SPATIAL][IoT] reportId={} — {} trạm IoT lân cận, đang phân tích.",
                reportId, iotEvents.size());

        boolean anyFlooding = iotEvents.stream().anyMatch(this::isFlooding);
        if (anyFlooding) {
            log.info("[SCORING][SPATIAL][IoT] reportId={} — trạm IoT xác nhận ngập → score=1.0",
                    reportId);
            return 1.0;
        }

        boolean anySafe = iotEvents.stream().anyMatch(this::isSafe);
        if (anySafe) {
            return evaluateSafeWithCrowdCheck(reportId, lat, lon, since);
        }

        // Trạm tồn tại nhưng không rõ trạng thái (thiếu dữ liệu, đang khởi động...)
        log.warn("[SCORING][SPATIAL][IoT] reportId={} — Trạm IoT hiện diện nhưng không xác định trạng thái → score=0.0",
                reportId);
        return 0.0;
    }

    /**
     * Nhánh xử lý khi trạm IoT báo KHÔNG NGẬP:
     * hỏi lại đám đông để quyết định có nên tin máy hay không.
     *
     * <ul>
     *   <li>Đám đông < 3 -> Xác suất báo cáo giả cao, tin máy móc → 0.0.</li>
     *   <li>Đám đông ≥ 3 -> Bất thường thống kê! Máy móc có thể hỏng.
     *       Đám đông phế truất quyền phủ quyết -> lấy điểm đám đông (100.0).</li>
     * </ul>
     */
    private double evaluateSafeWithCrowdCheck(String reportId, double lat, double lon, LocalDateTime since) {
        int crowdCount = floodEventRepository.countNearbyActiveUserReports(lat, lon, RADIUS_KM, since);

        log.info("[SCORING][SPATIAL][IoT-SAFE] reportId={} — Máy báo không ngập. Kiểm tra đám đông: {} người.",
                reportId, crowdCount);

        if (crowdCount < 3) {
            log.info("[SCORING][SPATIAL][IoT-SAFE] reportId={} — Đám đông nhỏ ({} người) → Tin máy móc → score=0.0",
                    reportId, crowdCount);
            return 0.0;
        }

        log.warn("[SCORING][SPATIAL][IoT-SAFE] reportId={} — ĐÁM ĐÔNG PHẾ TRUẤT MÁY MÓC! "
                        + "{} người báo ngập, trạm IoT có thể hỏng → chuyển sang điểm đám đông.",
                reportId, crowdCount);
        return evaluateCrowdConsensus(reportId, lat, lon, since);
    }

    /**
     * Bước 2: Tính điểm đồng thuận dựa trên số lượng USER_REPORT active lân cận.
     */
    private double evaluateCrowdConsensus(String reportId, double lat, double lon, LocalDateTime since) {
        int count = floodEventRepository.countNearbyActiveUserReports(lat, lon, RADIUS_KM, since);

        log.info("[SCORING][SPATIAL][CROWD] reportId={} — {} báo cáo đồng thuận trong {}km/{} phút.",
                reportId, count, RADIUS_KM, TIME_WINDOW_MINUTES);

        double score = switch (count) {
            case 0 -> {
                log.info("[SCORING][SPATIAL][CROWD] reportId={} — Người tiên phong đơn độc → score=0.0", reportId);
                yield 0.0;
            }
            case 1 -> {
                log.info("[SCORING][SPATIAL][CROWD] reportId={} — 1 báo cáo xác nhận → score=50.0", reportId);
                yield 0.5;
            }
            case 2 -> {
                log.info("[SCORING][SPATIAL][CROWD] reportId={} — 2 báo cáo xác nhận → score=80.0", reportId);
                yield 0.8;
            }
            default -> {
                log.info("[SCORING][SPATIAL][CROWD] reportId={} — Đám đông ({}) xác nhận → score=100.0", reportId, count);
                yield 1.0;
            }
        };

        return score;
    }

    /**
     * Trả về true nếu sự kiện IoT cho thấy đang có ngập (WARNING/DANGER hoặc mực nước vượt ngưỡng).
     */
    private boolean isFlooding(FloodEvent event) {
        boolean bySeverity = event.getSeverityLevel() != null
                && FLOODING_SEVERITIES.contains(event.getSeverityLevel().toUpperCase());
        boolean byWaterLevel = event.getWaterLevel() != null
                && event.getWaterLevel().compareTo(FLOOD_THRESHOLD) > 0;
        return bySeverity || byWaterLevel;
    }

    /**
     * Trả về true nếu sự kiện IoT xác nhận không ngập (SAFE hoặc mực nước = 0).
     */
    private boolean isSafe(FloodEvent event) {
        boolean bySeverity = event.getSeverityLevel() != null
                && SAFE_SEVERITY.equalsIgnoreCase(event.getSeverityLevel());
        boolean byWaterLevel = event.getWaterLevel() != null
                && event.getWaterLevel().compareTo(BigDecimal.ZERO) == 0;
        return bySeverity || byWaterLevel;
    }
}
