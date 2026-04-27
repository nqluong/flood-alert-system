package org.project.floodalert.floodcore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchCreateSensorRequest {

    @NotEmpty(message = "Danh sách sensor không được để trống")
    @Size(min = 1, max = 50, message = "Số lượng sensor phải từ 1 đến 50")
    @Valid
    List<CreateSensorRequest> sensors;
}
