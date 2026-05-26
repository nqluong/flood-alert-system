package org.project.floodalert.floodcore.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodcore.dto.internal.FloodDetail;
import org.project.floodalert.floodcore.dto.request.SafeRouteRequest;
import org.project.floodalert.floodcore.enums.VehicleType;
import org.project.floodalert.floodcore.service.saferoute.FloodSpatialQueryService;
import org.project.floodalert.floodcore.service.saferoute.OrsRouteClient;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SafeRoutingServiceImplTest {

    @Mock
    private FloodSpatialQueryService floodSpatialQueryService;

    @Mock
    private OrsRouteClient orsRouteClient;

    @InjectMocks
    private SafeRoutingServiceImpl safeRoutingService;

    private SafeRouteRequest buildRequest(VehicleType vehicleType) {
        return SafeRouteRequest.builder()
                .startLat(10.7).startLon(106.6)
                .endLat(10.8).endLon(106.7)
                .vehicleType(vehicleType)
                .build();
    }

    @Test
    void findSafeRoute_noFloodsInBbox_callsOrsWithoutAvoid() {
        when(floodSpatialQueryService.findFloodIdsInBoundingBox(any())).thenReturn(Collections.emptyList());
        when(orsRouteClient.call(any(), any())).thenReturn("{}");

        safeRoutingService.findSafeRoute(buildRequest(VehicleType.MOTORBIKE));
    }

    @Test
    void findSafeRoute_floodsFoundButBelowThreshold_noDangerousFloods() {
        when(floodSpatialQueryService.findFloodIdsInBoundingBox(any())).thenReturn(List.of("event-1"));
        FloodDetail safeFlood = FloodDetail.builder()
                .eventId("event-1").lat(10.75).lon(106.65)
                .waterLevel(5.0)
                .build();
        when(floodSpatialQueryService.batchGetFloodDetails(anyList())).thenReturn(List.of(safeFlood));
        when(orsRouteClient.call(any(), any())).thenReturn("{}");

        safeRoutingService.findSafeRoute(buildRequest(VehicleType.MOTORBIKE));
    }

    @Test
    void findSafeRoute_floodWithNullWaterLevel_filteredOut() {
        when(floodSpatialQueryService.findFloodIdsInBoundingBox(any())).thenReturn(List.of("event-1"));
        FloodDetail flood = FloodDetail.builder()
                .eventId("event-1").lat(10.75).lon(106.65)
                .waterLevel(null)
                .build();
        when(floodSpatialQueryService.batchGetFloodDetails(anyList())).thenReturn(List.of(flood));
        when(orsRouteClient.call(any(), any())).thenReturn("{}");

        safeRoutingService.findSafeRoute(buildRequest(VehicleType.CAR));
    }

    @Test
    void findSafeRoute_dangerousFloods_buildsAvoidPolygons() {
        when(floodSpatialQueryService.findFloodIdsInBoundingBox(any())).thenReturn(List.of("event-1"));
        FloodDetail dangerFlood = FloodDetail.builder()
                .eventId("event-1").lat(10.75).lon(106.65)
                .waterLevel(50.0)
                .build();
        when(floodSpatialQueryService.batchGetFloodDetails(anyList())).thenReturn(List.of(dangerFlood));
        when(orsRouteClient.call(any(), anyList())).thenReturn("{}");

        safeRoutingService.findSafeRoute(buildRequest(VehicleType.MOTORBIKE));
    }
}
