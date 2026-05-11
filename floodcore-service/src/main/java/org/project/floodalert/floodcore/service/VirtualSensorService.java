package org.project.floodalert.floodcore.service;

import org.project.floodalert.floodcore.dto.request.VirtualSensorProvisionRequest;
import org.project.floodalert.floodcore.dto.response.VirtualSensorCleanupResponse;
import org.project.floodalert.floodcore.dto.response.VirtualSensorProvisionResponse;

public interface VirtualSensorService {

    VirtualSensorProvisionResponse provisionVirtualSensors(VirtualSensorProvisionRequest request);

    VirtualSensorCleanupResponse cleanupVirtualSensors();
}
