package org.project.floodalert.floodcore.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateSensorRequest {
    // Thông tin cơ bản
    @Size(max = 255, message = "Tên cảm biến không được vượt quá 255 ký tự")
    String name;

    @Size(max = 1000, message = "Địa điểm không được vượt quá 1000 ký tự")
    String locationName;

    // Vị trí (nếu di chuyển sensor)
    @DecimalMin(value = "-90.0", message = "Vĩ độ phải từ -90 đến 90")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải từ -90 đến 90")
    @Digits(integer = 2, fraction = 8, message = "Vĩ độ không hợp lệ")
    BigDecimal lat;

    @DecimalMin(value = "-180.0", message = "Kinh độ phải từ -180 đến 180")
    @DecimalMax(value = "180.0", message = "Kinh độ phải từ -180 đến 180")
    @Digits(integer = 3, fraction = 8, message = "Kinh độ không hợp lệ")
    BigDecimal lon;

    // Ngưỡng cảnh báo
    @DecimalMin(value = "0.0", message = "Ngưỡng cảnh báo phải lớn hơn 0")
    @Digits(integer = 3, fraction = 2, message = "Ngưỡng cảnh báo không hợp lệ")
    BigDecimal warningThreshold;

    @DecimalMin(value = "0.0", message = "Ngưỡng nguy hiểm phải lớn hơn 0")
    @Digits(integer = 3, fraction = 2, message = "Ngưỡng nguy hiểm không hợp lệ")
    BigDecimal dangerThreshold;

    // Thông tin phần cứng
    @Size(max = 100, message = "Model phần cứng không được vượt quá 100 ký tự")
    String hardwareModel;

    @Size(max = 50, message = "Phiên bản firmware không được vượt quá 50 ký tự")
    String firmwareVersion;

    // Comment từ admin (tùy chọn)
    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    String comment;
}
