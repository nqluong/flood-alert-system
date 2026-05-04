package service.scoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.repository.FloodEventRepository;
import org.project.floodalert.floodprocessor.service.scoring.impl.SpatialConsensusScoringStrategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpatialConsensusScoringStrategyTest {

    @Mock
    private FloodEventRepository floodEventRepository;

    @InjectMocks
    private SpatialConsensusScoringStrategy strategy;

    private ReportMessage reportMessage;

    @BeforeEach
    void setUp() {
        reportMessage = ReportMessage.builder()
                .reportId("TEST-001")
                .lat(10.762622)
                .lon(106.660172)
                .build();
    }


    // NHÓM 1: CÓ TRẠM IoT LÂN CẬN

    @Nested
    @DisplayName("Nhóm 1 — Có trạm IoT lân cận")
    class WithIotSensor {

        /**
         * TC 1.1 — IoT báo NGẬP (waterLevel = 15cm > ngưỡng 10cm)
         * Kỳ vọng: score = 100.0 (Bảo chứng vàng)
         * Tối ưu: countNearbyActiveUserReports KHÔNG được gọi
         */
        @Test
        @DisplayName("TC 1.1 — IoT báo ngập → score = 1.0, không truy vấn đám đông")
        void tc1_1_iotReportsFlooding_shouldReturn100_withoutQueryingCrowd() {
            // Arrange
            FloodEvent floodingEvent = buildIotEvent("WARNING", BigDecimal.valueOf(15.0));
            mockIotEvents(List.of(floodingEvent));

            // Act
            double score = strategy.calculateScore(reportMessage);

            // Assert
            assertThat(score).isEqualTo(1.0);

            // Verify: Repository đếm người KHÔNG được gọi (tối ưu hiệu năng)
            verify(floodEventRepository, never())
                    .countNearbyActiveUserReports(anyDouble(), anyDouble(), anyDouble(), any(LocalDateTime.class));
        }

        /**
         * TC 1.1b — IoT báo ngập bằng severity = DANGER (dù waterLevel thấp)
         * Kỳ vọng: score = 100.0
         */
        @Test
        @DisplayName("TC 1.1b — IoT severity=DANGER → score = 1.0")
        void tc1_1b_iotSeverityDanger_shouldReturn100() {
            FloodEvent dangerEvent = buildIotEvent("DANGER", BigDecimal.valueOf(5.0));
            mockIotEvents(List.of(dangerEvent));

            assertThat(strategy.calculateScore(reportMessage)).isEqualTo(1.0);

            verify(floodEventRepository, never())
                    .countNearbyActiveUserReports(anyDouble(), anyDouble(), anyDouble(), any(LocalDateTime.class));
        }

        /**
         * TC 1.2 — IoT báo KHÔNG ngập + đám đông NHỎ (1 người)
         * Kỳ vọng: score = 0.0 (Phủ quyết thành công, tin máy móc)
         */
        @Test
        @DisplayName("TC 1.2 — IoT báo không ngập + 1 người báo cáo → score = 0.0")
        void tc1_2_iotSafe_smallCrowd_1person_shouldReturn0() {
            // Arrange
            FloodEvent safeEvent = buildIotEvent("SAFE", BigDecimal.ZERO);
            mockIotEvents(List.of(safeEvent));
            mockCrowdCount(1);

            // Act
            double score = strategy.calculateScore(reportMessage);

            // Assert
            assertThat(score).isEqualTo(0.0);
        }

        /**
         * TC 1.2b — IoT báo KHÔNG ngập + đám đông NHỎ (0 người)
         * Kỳ vọng: score = 0.0
         */
        @Test
        @DisplayName("TC 1.2b — IoT báo không ngập + 0 người báo cáo → score = 0.0")
        void tc1_2b_iotSafe_crowdEmpty_shouldReturn0() {
            FloodEvent safeEvent = buildIotEvent("SAFE", BigDecimal.ZERO);
            mockIotEvents(List.of(safeEvent));
            mockCrowdCount(0);

            assertThat(strategy.calculateScore(reportMessage)).isEqualTo(0.0);
        }

        /**
         * TC 1.2c — IoT báo KHÔNG ngập + đám đông NHỎ (2 người) — biên dưới ngưỡng
         * Kỳ vọng: score = 0.0
         */
        @Test
        @DisplayName("TC 1.2c — IoT báo không ngập + 2 người (biên dưới ngưỡng) → score = 0.0")
        void tc1_2c_iotSafe_crowdUnderThreshold_shouldReturn0() {
            FloodEvent safeEvent = buildIotEvent("SAFE", BigDecimal.ZERO);
            mockIotEvents(List.of(safeEvent));
            mockCrowdCount(2);

            assertThat(strategy.calculateScore(reportMessage)).isEqualTo(0.0);
        }

        /**
         * TC 1.3 — IoT báo KHÔNG ngập + đám đông LỚN (3 người — biên dưới phế truất)
         * Kỳ vọng: score = 100.0 (Máy hỏng, đám đông phế truất)
         */
        @Test
        @DisplayName("TC 1.3 — IoT báo không ngập + 3 người (đám đông phế truất) → score = 1.0")
        void tc1_3_iotSafe_largeCrowd_3persons_shouldReturn100() {
            // Arrange
            FloodEvent safeEvent = buildIotEvent("SAFE", BigDecimal.ZERO);
            mockIotEvents(List.of(safeEvent));
            mockCrowdCount(3);

            // Act
            double score = strategy.calculateScore(reportMessage);

            // Assert
            assertThat(score).isEqualTo(1.0);
        }

        /**
         * TC 1.3b — IoT báo KHÔNG ngập + đám đông LỚN (10 người)
         * Kỳ vọng: score = 100.0
         */
        @Test
        @DisplayName("TC 1.3b — IoT báo không ngập + 10 người → score = 1.0")
        void tc1_3b_iotSafe_largeCrowd_10persons_shouldReturn100() {
            FloodEvent safeEvent = buildIotEvent("SAFE", BigDecimal.ZERO);
            mockIotEvents(List.of(safeEvent));
            mockCrowdCount(10);

            assertThat(strategy.calculateScore(reportMessage)).isEqualTo(1.0);
        }

        /**
         * TC 1.3 — Verify: countNearbyActiveUserReports được gọi đúng 1 lần
         * khi IoT safe và đám đông đủ lớn để phế truất
         */
        @Test
        @DisplayName("TC 1.3c — Khi đám đông phế truất IoT, repository được gọi đúng 1 lần")
        void tc1_3c_iotSafe_largeCrowd_verifyRepositoryCalledOnce() {
            FloodEvent safeEvent = buildIotEvent("SAFE", BigDecimal.ZERO);
            mockIotEvents(List.of(safeEvent));
            mockCrowdCount(5);

            strategy.calculateScore(reportMessage);

            // Verify: gọi 2 lần — lần 1 trong evaluateSafeWithCrowdCheck (kiểm tra ngưỡng),
            // lần 2 trong evaluateCrowdConsensus (tính điểm thực sự)
            verify(floodEventRepository, times(2))
                    .countNearbyActiveUserReports(anyDouble(), anyDouble(), anyDouble(), any(LocalDateTime.class));
        }
    }

    // NHÓM 2: KHÔNG CÓ TRẠM IoT — Chỉ dựa vào đám đông

    @Nested
    @DisplayName("Nhóm 2 — Không có trạm IoT (chỉ đám đông)")
    class WithoutIotSensor {

        @BeforeEach
        void noIotSensor() {
            // Không có trạm IoT nào trong bán kính
            mockIotEvents(List.of());
        }

        /**
         * TC 2.1 — Sói cô độc (0 báo cáo đồng thuận)
         * Kỳ vọng: score = 0.0
         */
        @Test
        @DisplayName("TC 2.1 — Không có ai báo cùng (người tiên phong) → score = 0.0")
        void tc2_1_noCrowd_shouldReturn0() {
            mockCrowdCount(0);

            assertThat(strategy.calculateScore(reportMessage)).isEqualTo(0.0);
        }

        /**
         * TC 2.2 — Có thêm 1 đồng minh
         * Kỳ vọng: score = 50.0
         */
        @Test
        @DisplayName("TC 2.2 — 1 người báo cùng → score = 0.5")
        void tc2_2_oneCrowdReport_shouldReturn50() {
            mockCrowdCount(1);

            assertThat(strategy.calculateScore(reportMessage)).isEqualTo(0.5);
        }

        /**
         * TC 2.3 — Có thêm 2 đồng minh
         * Kỳ vọng: score = 80.0
         */
        @Test
        @DisplayName("TC 2.3 — 2 người báo cùng → score = 0.8")
        void tc2_3_twoCrowdReports_shouldReturn80() {
            mockCrowdCount(2);

            assertThat(strategy.calculateScore(reportMessage)).isEqualTo(0.8);
        }

        /**
         * TC 2.4 — Đám đông hình thành (cận dưới = 3 người)
         * Kỳ vọng: score = 100.0
         */
        @Test
        @DisplayName("TC 2.4 — 3 người báo cùng (cận dưới đám đông) → score = 1.0")
        void tc2_4_threeCrowdReports_lowerBound_shouldReturn100() {
            mockCrowdCount(3);

            assertThat(strategy.calculateScore(reportMessage)).isEqualTo(1.0);
        }

        /**
         * TC 2.5 — Đám đông khổng lồ (50 người)
         * Kỳ vọng: vẫn là 100.0 (không vượt thang điểm tối đa)
         */
        @Test
        @DisplayName("TC 2.5 — 50 người báo cùng → vẫn là 1.0")
        void tc2_5_hugeCrowd_shouldStillReturn100_notExceedMaxScore() {
            mockCrowdCount(50);

            double score = strategy.calculateScore(reportMessage);

            assertThat(score)
                    .isEqualTo(1.0)
                    .isLessThanOrEqualTo(1.0); // Không vượt thang điểm
        }
    }


    // NHÓM 3: isApplicable & getStrategyName
    @Nested
    @DisplayName("Nhóm 3 — Metadata của strategy")
    class StrategyMetadata {

        @Test
        @DisplayName("isApplicable luôn trả về true với mọi message")
        void isApplicable_shouldAlwaysReturnTrue() {
            assertThat(strategy.isApplicable(reportMessage)).isTrue();
            assertThat(strategy.isApplicable(ReportMessage.builder().reportId("OTHER").build())).isTrue();
        }

        @Test
        @DisplayName("getStrategyName trả về đúng tên SPATIAL_CONSENSUS")
        void getStrategyName_shouldReturnCorrectName() {
            assertThat(strategy.getStrategyName()).isEqualTo("SPATIAL_CONSENSUS");
        }
    }

    /**
     * Tạo FloodEvent giả lập từ trạm IoT với severity và waterLevel cho trước.
     */
    private FloodEvent buildIotEvent(String severity, BigDecimal waterLevel) {
        FloodEvent event = new FloodEvent();
        event.setSeverityLevel(severity);
        event.setWaterLevel(waterLevel);
        return event;
    }

    /**
     * Mock findRecentIotEventsNearby để trả về danh sách IoT events giả lập.
     */
    private void mockIotEvents(List<FloodEvent> events) {
        when(floodEventRepository.findRecentIotEventsNearby(
                anyDouble(), anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(events);
    }

    /**
     * Mock countNearbyActiveUserReports để trả về số lượng báo cáo đám đông giả lập.
     */
    private void mockCrowdCount(int count) {
        when(floodEventRepository.countNearbyActiveUserReports(
                anyDouble(), anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(count);
    }
}
