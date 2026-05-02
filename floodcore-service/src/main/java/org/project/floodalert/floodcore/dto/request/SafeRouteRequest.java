package org.project.floodalert.floodcore.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.project.floodalert.floodcore.enums.VehicleType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SafeRouteRequest {

    @NotNull(message = "Vĩ độ điểm xuất phát không được để trống")
    Double startLat;

    @NotNull(message = "Kinh độ điểm xuất phát không được để trống")
    Double startLon;

    @NotNull(message = "Vĩ độ điểm đến không được để trống")
    Double endLat;

    @NotNull(message = "Kinh độ điểm đến không được để trống")
    Double endLon;

    @NotNull(message = "Loại phương tiện không được để trống")
    VehicleType vehicleType;

}
