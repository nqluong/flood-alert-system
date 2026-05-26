package org.project.floodalert.floodcore.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodcore.dto.response.ActiveFloodResponse;
import org.project.floodalert.floodcore.dto.response.SafeRouteResponse;
import org.project.floodalert.floodcore.enums.VehicleType;
import org.project.floodalert.floodcore.service.FloodGeoCache;
import org.project.floodalert.floodcore.service.SafeRoutingService;
import org.project.floodalert.floodcore.service.UserReportService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapApiControllerTest {

    @Mock
    private FloodGeoCache floodGeoCache;

    @Mock
    private UserReportService userReportService;

    @Mock
    private SafeRoutingService safeRoutingService;

    @InjectMocks
    private MapApiController mapApiController;

    private ActiveFloodResponse activeFloodResponse;
    private SafeRouteResponse safeRouteResponse;

    @BeforeEach
    void setUp() {
        activeFloodResponse = mock(ActiveFloodResponse.class);
        safeRouteResponse = mock(SafeRouteResponse.class);
    }

    @Test
    void getNearbyActiveFloods_callsFloodGeoCache() {
        when(floodGeoCache.findFloodsInRadius(10.776, 106.700, 5.0))
                .thenReturn(List.of(activeFloodResponse));

        mapApiController.getNearbyActiveFloods(10.776, 106.700, 5.0);

        verify(floodGeoCache).findFloodsInRadius(10.776, 106.700, 5.0);
    }

    @Test
    void getNearbyActiveFloods_emptyList_callsFloodGeoCache() {
        when(floodGeoCache.findFloodsInRadius(10.776, 106.700, 10.0))
                .thenReturn(List.of());

        mapApiController.getNearbyActiveFloods(10.776, 106.700, 10.0);

        verify(floodGeoCache).findFloodsInRadius(10.776, 106.700, 10.0);
    }

    @Test
    void getSafePath_motorbike_callsSafeRoutingService() {
        when(safeRoutingService.findSafeRoute(any()))
                .thenReturn(safeRouteResponse);

        mapApiController.getSafePath(
                10.776,
                106.700,
                10.780,
                106.710,
                VehicleType.MOTORBIKE
        );

        verify(safeRoutingService).findSafeRoute(any());
    }

    @Test
    void getSafePath_car_callsSafeRoutingService() {
        when(safeRoutingService.findSafeRoute(any()))
                .thenReturn(safeRouteResponse);

        mapApiController.getSafePath(
                10.776,
                106.700,
                10.780,
                106.710,
                VehicleType.CAR
        );

        verify(safeRoutingService).findSafeRoute(any());
    }

    @Test
    void getSafePath_serviceThrows_stillCallsService() {
        when(safeRoutingService.findSafeRoute(any()))
                .thenThrow(new RuntimeException("Routing error"));

        try {
            mapApiController.getSafePath(
                    10.776,
                    106.700,
                    10.780,
                    106.710,
                    VehicleType.MOTORBIKE
            );
        } catch (RuntimeException ignored) {
        }

        verify(safeRoutingService).findSafeRoute(any());
    }
}