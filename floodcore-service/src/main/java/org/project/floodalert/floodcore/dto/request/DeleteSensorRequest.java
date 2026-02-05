package org.project.floodalert.floodcore.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeleteSensorRequest {
    @Size(min = 10, max = 500, message = "Lý do phải từ 10 đến 500 ký tự")
    String reason;

    /**
     * true = Xóa khỏi Redis Geo (không hiển thị trên map)
     * false = Giữ lại nhưng đánh dấu DISABLED
     */
    @Builder.Default
    Boolean removeFromMap = true;
}
