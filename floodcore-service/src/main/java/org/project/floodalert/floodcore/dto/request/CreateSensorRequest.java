package org.project.floodalert.floodcore.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateSensorRequest {

    @NotBlank(message = "Mã cảm biến không được để trống")
    @Size(max = 50, message = "Mã cảm biến không được vượt quá 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Mã cảm biến chỉ chứa chữ in hoa, số và dấu gạch ngang")
    String sensorId;

    @NotBlank(message = "Tên cảm biến không được để trống")
    @Size(max = 255, message = "Tên cảm biến không được vượt quá 255 ký tự")
    String name;

    @Size(max = 1000, message = "Địa điểm không được vượt quá 1000 ký tự")
    String locationName;

    @NotNull(message = "Vĩ độ không được để trống")
    @DecimalMin(value = "-90.0", message = "Vĩ độ phải từ -90 đến 90")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải từ -90 đến 90")
    @Digits(integer = 2, fraction = 8, message = "Vĩ độ không hợp lệ")
    BigDecimal lat;

    @NotNull(message = "Kinh độ không được để trống")
    @DecimalMin(value = "-180.0", message = "Kinh độ phải từ -180 đến 180")
    @DecimalMax(value = "180.0", message = "Kinh độ phải từ -180 đến 180")
    @Digits(integer = 3, fraction = 8, message = "Kinh độ không hợp lệ")
    BigDecimal lon;

    @NotNull(message = "Ngưỡng cảnh báo không được để trống")
    @DecimalMin(value = "0.0", message = "Ngưỡng cảnh báo phải lớn hơn 0")
    @Digits(integer = 3, fraction = 2, message = "Ngưỡng cảnh báo không hợp lệ")
    BigDecimal warningThreshold;

    @NotNull(message = "Ngưỡng nguy hiểm không được để trống")
    @DecimalMin(value = "0.0", message = "Ngưỡng nguy hiểm phải lớn hơn 0")
    @Digits(integer = 3, fraction = 2, message = "Ngưỡng nguy hiểm không hợp lệ")
    BigDecimal dangerThreshold;

    @Size(max = 100, message = "Model phần cứng không được vượt quá 100 ký tự")
    String hardwareModel;

    @Size(max = 50, message = "Phiên bản firmware không được vượt quá 50 ký tự")
    String firmwareVersion;
}
