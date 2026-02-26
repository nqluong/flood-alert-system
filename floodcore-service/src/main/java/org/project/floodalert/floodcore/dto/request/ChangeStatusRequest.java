package org.project.floodalert.floodcore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangeStatusRequest {

    /**
     * Trạng thái mới muốn chuyển sang.
     * Giá trị hợp lệ: ACTIVE, DISABLED, MAINTENANCE, OFFLINE
     */
    @NotNull(message = "Trạng thái mới không được để trống")
    @NotBlank(message = "Trạng thái mới không được rỗng")
    String newStatus;

    /**
     * Lý do chuyển trạng thái (bắt buộc để audit)
     */
    @NotNull(message = "Lý do chuyển trạng thái không được để trống")
    @Size(min = 5, max = 500, message = "Lý do phải từ 5 đến 500 ký tự")
    String reason;

    /**
     * Ghi chú thêm (tuỳ chọn)
     */
    @Size(max = 500, message = "Ghi chú không vượt quá 500 ký tự")
    String comment;
}
