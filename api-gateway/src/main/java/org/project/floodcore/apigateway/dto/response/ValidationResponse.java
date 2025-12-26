package org.project.floodcore.apigateway.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ValidationResponse {
    String field;
    String message;
    Object rejectedValue;
}
