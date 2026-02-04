package org.project.floodalert.floodcore.dto.request;

import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SensorFilterRequest {
    @Min(value = 0, message = "Số trang phải lớn hơn hoặc bằng 0")
    @Builder.Default
    Integer page = 0;

    @Min(value = 1, message = "Kích thước trang phải lớn hơn 0")
    @Builder.Default
    Integer size = 20;

    // Filter theo status
    String status; // ACTIVE, DISABLED, MAINTENANCE, OFFLINE

    // Tìm kiếm theo tên hoặc sensor ID
    String search;

    // Filter theo khu vực (có thể mở rộng sau)
    String region;

    // Sắp xếp
    @Builder.Default
    String sortBy = "createdAt";

    @Builder.Default
    String sortDirection = "DESC"; // ASC hoặc DESC
}
