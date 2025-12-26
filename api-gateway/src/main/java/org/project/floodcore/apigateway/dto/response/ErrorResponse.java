package org.project.floodcore.apigateway.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ErrorResponse {
    boolean success = false;
    int code;
    String message;
    String details;
    String path;

    @Builder.Default
    LocalDateTime timestamp = LocalDateTime.now();
    List<ValidationResponse> validationErrors;
}
