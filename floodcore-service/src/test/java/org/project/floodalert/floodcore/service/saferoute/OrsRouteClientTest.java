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
import org.project.floodalert.floodcore.exception.CoreErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private void stubOrsErrorBody(String body) throws Exception {
        when(objectMapper.readTree(body)).thenReturn(new ObjectMapper().readTree(body));
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
        AppException ex = assertThrows(AppException.class,
                () -> orsRouteClient.call(buildRequest(VehicleType.CAR), null));
        assertEquals(CoreErrorCode.EXTERNAL_SERVICE_ERROR, ex.getErrorCode());
    }

    // --- ORS routing error code mapping (error.code trong response body) ---

    @Test
    void call_badRequest_routeNotFound_mapsToRouteNotFound() throws Exception {
        String body = "{\"error\":{\"code\":2009,\"message\":\"Route could not be found\"}}";
        stubOrsErrorBody(body);
        doThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request",
                HttpHeaders.EMPTY, body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8))
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        AppException ex = assertThrows(AppException.class,
                () -> orsRouteClient.call(buildRequest(VehicleType.CAR), null));
        assertEquals(CoreErrorCode.ROUTE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void call_badRequest_pointNotFound_mapsToRoutePointNotFound() throws Exception {
        String body = "{\"error\":{\"code\":2010,\"message\":\"Point was not found\"}}";
        stubOrsErrorBody(body);
        doThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request",
                HttpHeaders.EMPTY, body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8))
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        AppException ex = assertThrows(AppException.class,
                () -> orsRouteClient.call(buildRequest(VehicleType.CAR), null));
        assertEquals(CoreErrorCode.ROUTE_POINT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void call_badRequest_entryExitNotReached_mapsToRoutePointNotAccessible() throws Exception {
        String body = "{\"error\":{\"code\":2016,\"message\":\"No route between entry and exit found\"}}";
        stubOrsErrorBody(body);
        doThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request",
                HttpHeaders.EMPTY, body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8))
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        AppException ex = assertThrows(AppException.class,
                () -> orsRouteClient.call(buildRequest(VehicleType.CAR), null));
        assertEquals(CoreErrorCode.ROUTE_POINT_NOT_ACCESSIBLE, ex.getErrorCode());
    }

    @Test
    void call_badRequest_unmappedOrsCode_fallsBackToExternalServiceError() throws Exception {
        String body = "{\"error\":{\"code\":2012,\"message\":\"Unknown parameter\"}}";
        stubOrsErrorBody(body);
        doThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request",
                HttpHeaders.EMPTY, body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8))
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        AppException ex = assertThrows(AppException.class,
                () -> orsRouteClient.call(buildRequest(VehicleType.CAR), null));
        assertEquals(CoreErrorCode.EXTERNAL_SERVICE_ERROR, ex.getErrorCode());
    }

    @Test
    void call_serverError_unknownInternalError_mapsToRouteNotFound() throws Exception {
        String body = "{\"error\":{\"code\":2099,\"message\":\"Unknown internal error\"}}";
        stubOrsErrorBody(body);
        doThrow(HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                HttpHeaders.EMPTY, body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8))
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        AppException ex = assertThrows(AppException.class,
                () -> orsRouteClient.call(buildRequest(VehicleType.CAR), null));
        assertEquals(CoreErrorCode.ROUTE_NOT_FOUND, ex.getErrorCode());
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
