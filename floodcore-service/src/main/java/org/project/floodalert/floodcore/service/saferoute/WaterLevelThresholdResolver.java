package org.project.floodalert.floodcore.service.saferoute;

import lombok.experimental.UtilityClass;
import org.project.floodalert.floodcore.enums.VehicleType;

import java.util.Map;

@UtilityClass
public class WaterLevelThresholdResolver {

    public static final double MOTORBIKE_THRESHOLD_CM = 15.0;
    public static final double CAR_THRESHOLD_CM = 30.0;

    private static final Map<VehicleType, Double> THRESHOLDS = Map.of(
            VehicleType.MOTORBIKE, MOTORBIKE_THRESHOLD_CM,
            VehicleType.CAR, CAR_THRESHOLD_CM
    );

    public static double resolve(VehicleType vehicleType) {
        return THRESHOLDS.getOrDefault(vehicleType, CAR_THRESHOLD_CM);
    }
}
