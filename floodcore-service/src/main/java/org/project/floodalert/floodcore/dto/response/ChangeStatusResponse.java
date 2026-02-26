package org.project.floodalert.floodcore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangeStatusResponse {

    UUID id;
    String sensorId;
    String name;

    /** Trạng thái cũ trước khi chuyển */
    String previousStatus;

    /** Trạng thái mới sau khi chuyển */
    String currentStatus;

    /** Các trạng thái có thể chuyển tiếp từ trạng thái hiện tại */
    List<String> allowedNextStatuses;

    /** Thông tin về ảnh hưởng đến Redis / blacklist */
    boolean syncedToRedis;
    boolean addedToBlacklist;

    String message;
    LocalDateTime changedAt;
}
