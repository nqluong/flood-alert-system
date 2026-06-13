package org.project.floodalert.floodcore.service.saferoute;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.exception.AppException;
import org.slf4j.LoggerFactory;
import org.project.floodalert.floodcore.config.OrsProperties;
import org.project.floodalert.floodcore.dto.ors.OrsRequest;
import org.project.floodalert.floodcore.dto.request.SafeRouteRequest;
import org.project.floodalert.floodcore.enums.VehicleType;
import org.project.floodalert.floodcore.exception.CoreErrorCode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrsRouteClient {

    private static final org.slf4j.Logger GEOJSON_LOG = LoggerFactory.getLogger("geojson.ors.response");
    private static final List<String> MOTORBIKE_AVOID_FEATURES = List.of("highways", "tollways");

    private final OrsProperties orsProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public String call(SafeRouteRequest request, List<List<List<List<Double>>>> avoidPolygons) {
        OrsRequest orsRequest = buildOrsRequest(request, avoidPolygons);
        logPayloadIfDebug(orsRequest);

        HttpEntity<OrsRequest> entity = new HttpEntity<>(orsRequest, buildHeaders());
        log.debug("[SafeRouting] Gọi ORS API: {}", orsProperties.getApiUrl());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    orsProperties.getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("[SafeRouting] ORS API trả về thành công");
                GEOJSON_LOG.info("{}", response.getBody());
                return response.getBody();
            }

            log.error("[SafeRouting] ORS API trả về status code: {}", response.getStatusCode());
            throw new AppException(CoreErrorCode.EXTERNAL_SERVICE_ERROR);

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.error("[SafeRouting] ORS API rate limit (429): {}", e.getMessage());
            throw new AppException(CoreErrorCode.EXTERNAL_SERVICE_ERROR);

        } catch (HttpClientErrorException.BadRequest e) {
            log.error("[SafeRouting] ORS API bad request (400): {}", e.getResponseBodyAsString());
            throw mapOrsErrorResponse(e.getResponseBodyAsString());

        } catch (ResourceAccessException e) {
            log.error("[SafeRouting] ORS API timeout: {}", e.getMessage());
            throw new AppException(CoreErrorCode.EXTERNAL_SERVICE_ERROR);

        } catch (HttpServerErrorException e) {
            log.error("[SafeRouting] ORS API lỗi server ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw mapOrsErrorResponse(e.getResponseBodyAsString());

        } catch (AppException e) {
            throw e;

        } catch (Exception e) {
            log.error("[SafeRouting] Lỗi khi gọi ORS API: {}", e.getMessage(), e);
            throw new AppException(CoreErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    private OrsRequest buildOrsRequest(SafeRouteRequest request, List<List<List<List<Double>>>> avoidPolygons) {
        OrsRequest.OrsOptions options = buildOptions(request.getVehicleType(), avoidPolygons);

        return OrsRequest.builder()
                .coordinates(List.of(
                        List.of(request.getStartLon(), request.getStartLat()),
                        List.of(request.getEndLon(), request.getEndLat())))
                .options(options)
                .build();
    }

    private OrsRequest.OrsOptions buildOptions(VehicleType vehicleType, List<List<List<List<Double>>>> avoidPolygons) {
        boolean hasAvoidPolygons = avoidPolygons != null && !avoidPolygons.isEmpty();
        boolean isMotorbike = VehicleType.MOTORBIKE.equals(vehicleType);

        if (!hasAvoidPolygons && !isMotorbike) {
            return null;
        }

        OrsRequest.OrsOptions.OrsOptionsBuilder builder = OrsRequest.OrsOptions.builder();

        if (hasAvoidPolygons) {
            builder.avoidPolygons(OrsRequest.AvoidPolygons.builder()
                    .coordinates(avoidPolygons)
                    .build());
        }

        if (isMotorbike) {
            builder.avoidFeatures(MOTORBIKE_AVOID_FEATURES);
        }

        return builder.build();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", orsProperties.getApiKey());
        return headers;
    }

    private void logPayloadIfDebug(OrsRequest orsRequest) {
        if (!log.isDebugEnabled())
            return;
        try {
            log.debug("[SafeRouting] ORS payload:\n{}",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(orsRequest));
        } catch (Exception e) {
            log.warn("[SafeRouting] Không thể serialize ORS payload: {}", e.getMessage());
        }
    }

    /**
     * Map mã lỗi routing (field error.code) trong response của ORS sang AppException
     * tương ứng, để mobile hiển thị được lý do cụ thể thay vì lỗi chung chung.
     */
    private AppException mapOrsErrorResponse(String responseBody) {
        return switch (extractOrsErrorCode(responseBody)) {
            case 2009, 2099 -> new AppException(CoreErrorCode.ROUTE_NOT_FOUND);
            case 2010 -> new AppException(CoreErrorCode.ROUTE_POINT_NOT_FOUND);
            case 2013, 2014, 2015, 2016 -> new AppException(CoreErrorCode.ROUTE_POINT_NOT_ACCESSIBLE);
            default -> new AppException(CoreErrorCode.EXTERNAL_SERVICE_ERROR);
        };
    }

    private int extractOrsErrorCode(String responseBody) {
        try {
            return objectMapper.readTree(responseBody).path("error").path("code").asInt(-1);
        } catch (Exception e) {
            log.warn("[SafeRouting] Không thể parse mã lỗi ORS: {}", e.getMessage());
            return -1;
        }
    }
}
