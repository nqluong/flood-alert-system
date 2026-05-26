package org.project.floodalert.floodcore.service.saferoute;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodcore.config.OrsProperties;
import org.project.floodalert.floodcore.dto.request.SafeRouteRequest;
import org.project.floodalert.floodcore.enums.VehicleType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrsRouteClientTest {

    @Mock private OrsProperties orsProperties;
    @Mock private RestTemplate restTemplate;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private OrsRouteClient orsRouteClient;

    @BeforeEach
    void setUp() {
        when(orsProperties.getApiUrl()).thenReturn("https://ors.example.com/v2/directions/driving-car/geojson");
        when(orsProperties.getApiKey()).thenReturn("test-api-key");
    }

    private SafeRouteRequest buildRequest(VehicleType vehicleType) {
        return SafeRouteRequest.builder()
                .startLat(10.7).startLon(106.6)
                .endLat(10.8).endLon(106.7)
                .vehicleType(vehicleType)
                .build();
    }

    private List<List<List<Double>>> buildAvoidPolygons() {
        return List.of(List.of(
                List.of(106.5, 10.5), List.of(106.6, 10.5),
                List.of(106.6, 10.6), List.of(106.5, 10.5)));
    }

    // --- call success + buildOptions branches ---

    @Test
    void call_success_carWithNullPolygons() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));
        orsRouteClient.call(buildRequest(VehicleType.CAR), null);
    }

    @Test
    void call_success_carWithEmptyPolygons() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));
        orsRouteClient.call(buildRequest(VehicleType.CAR), List.of());
    }

    @Test
    void call_success_motorbikeWithNullPolygons() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));
        orsRouteClient.call(buildRequest(VehicleType.MOTORBIKE), null);
    }

    @Test
    void call_success_carWithAvoidPolygons() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));
        orsRouteClient.call(buildRequest(VehicleType.CAR), buildAvoidPolygons());
    }

    @Test
    void call_success_motorbikeWithAvoidPolygons() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));
        orsRouteClient.call(buildRequest(VehicleType.MOTORBIKE), buildAvoidPolygons());
    }

    // --- call error/exception paths ---

    @Test
    void call_nonOkStatus_throwsAppException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));
        assertThrows(AppException.class, () -> orsRouteClient.call(buildRequest(VehicleType.CAR), null));
    }

    @Test
    void call_nullBody_throwsAppException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(null));
        assertThrows(AppException.class, () -> orsRouteClient.call(buildRequest(VehicleType.CAR), null));
    }

    @Test
    void call_tooManyRequests_throwsAppException() {
        doThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8))
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));
        assertThrows(AppException.class, () -> orsRouteClient.call(buildRequest(VehicleType.CAR), null));
    }

    @Test
    void call_badRequest_throwsAppException() {
        doThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request",
                HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8))
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));
        assertThrows(AppException.class, () -> orsRouteClient.call(buildRequest(VehicleType.CAR), null));
    }

    @Test
    void call_resourceAccessException_throwsAppException() {
        doThrow(new ResourceAccessException("Connection timeout"))
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));
        assertThrows(AppException.class, () -> orsRouteClient.call(buildRequest(VehicleType.CAR), null));
    }

    @Test
    void call_genericException_throwsAppException() {
        doThrow(new RuntimeException("Unexpected error"))
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));
        assertThrows(AppException.class, () -> orsRouteClient.call(buildRequest(VehicleType.CAR), null));
    }
}
