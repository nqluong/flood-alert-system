package org.project.floodalert.floodprocessor.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.project.floodalert.floodprocessor.enums.FloodStatus;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProcessedSensorData extends EnrichedSensorData{
    FloodStatus status;

    FloodStatus previousStatus;

    @Builder.Default
    boolean stateChanged = false;
}
