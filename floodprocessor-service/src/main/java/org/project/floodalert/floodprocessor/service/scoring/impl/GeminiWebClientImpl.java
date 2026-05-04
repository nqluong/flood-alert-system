package org.project.floodalert.floodprocessor.service.scoring.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.config.GeminiProperties;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.dto.response.GeminiVisionResponse;
import org.project.floodalert.floodprocessor.service.scoring.GeminiApiClient;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class GeminiWebClientImpl implements GeminiApiClient {

    private static final String SYSTEM_INSTRUCTION =
            "You are an expert flood assessment AI. Analyze the image to determine if there is a flood. " +
                    "Return the result EXACTLY as a valid JSON object with NO markdown formatting. " +
                    "Schema: {\"is_flooded\": boolean, \"water_level_estimate\": \"NONE\" | \"ANKLE_DEEP\" | \"KNEE_DEEP\" | \"ABOVE_KNEE\", " +
                    "\"confidence_score\": integer (0-100, where 0 is fake/no flood, 100 is clear severe flood), " +
                    "\"reasoning\": \"Brief 1-sentence explanation\"}";

    private final WebClient geminiWebClient;
    private final WebClient imageDownloadClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    public GeminiWebClientImpl(GeminiProperties geminiProperties, ObjectMapper objectMapper) {
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(geminiProperties.getTimeoutSeconds()));

        this.geminiWebClient = WebClient.builder()
                .baseUrl(geminiProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .build();

        this.imageDownloadClient = WebClient.builder()
                .exchangeStrategies(strategies)
                .build();
    }

    @Override
    public GeminiVisionResponse analyzeFloodImage(ReportMessage msg) {
        log.info("[GEMINI] Bắt đầu phân tích ảnh: reportId={}, imageUrl={}", 
                msg.getReportId(), msg.getImageUrl());

        String base64Image = downloadAndEncodeImage(msg.getImageUrl());
        
        Map<String, Object> requestPayload = buildRequestPayload(
                base64Image,
                msg.getSeverityLevel(),
                msg.getDescription()
        );

        GeminiVisionResponse result = callGeminiApiWithRetry(msg.getReportId(), requestPayload);
        
        log.info("[GEMINI] Phân tích hoàn tất: reportId={}, isFlooded={}, confidence={}, waterLevel={}", 
                msg.getReportId(), result.isFlooded(), result.getConfidenceScore(), result.getWaterLevelEstimate());
        
        return result;
    }

    private GeminiVisionResponse callGeminiApiWithRetry(String reportId, Map<String, Object> requestPayload) {
        int maxAttempts = geminiProperties.getRetry().getMaxAttempts();
        long delayMs = geminiProperties.getRetry().getInitialDelayMs();
        
        String apiPath = buildApiPath();
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("[GEMINI] Attempt {}/{} - Gọi Gemini API cho reportId={}", 
                        attempt, maxAttempts, reportId);

                String responseBody = executeGeminiApiCall(apiPath, requestPayload);
                return parseGeminiResponse(responseBody);
                
            } catch (RetryableException e) {
                lastException = e;
                delayMs = handleRetryableError(attempt, maxAttempts, reportId, e, delayMs);
                
            } catch (IllegalStateException e) {
                lastException = e;
                delayMs = handleTimeoutError(attempt, maxAttempts, reportId, e, delayMs);

            } catch (Exception e) {
                handleNonRetryableError(reportId, e);
            }
        }
        
        throw buildMaxAttemptsException(maxAttempts, lastException);
    }

    private String buildApiPath() {
        return "/v1beta/models/" + geminiProperties.getModel()
                + ":generateContent?key=" + geminiProperties.getApiKey();
    }

    private String executeGeminiApiCall(String apiPath, Map<String, Object> requestPayload) {
        return geminiWebClient.post()
                .uri(apiPath)
                .bodyValue(requestPayload)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xxError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handle5xxError)
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(geminiProperties.getTimeoutSeconds()))
                .block();
    }

    private Mono<? extends Throwable> handle4xxError(
            ClientResponse clientResponse) {
        return clientResponse.bodyToMono(String.class).flatMap(body -> {
            int statusCode = clientResponse.statusCode().value();
            log.error("[GEMINI] Lỗi 4xx: status={}, body={}", statusCode, body);
            
            if (statusCode == 429) {
                return Mono.error(
                    new RetryableException("Rate limit: " + statusCode)
                );
            }
            
            return Mono.error(
                new RuntimeException("Gemini API client error: " + statusCode + " - " + body)
            );
        });
    }

    private Mono<? extends Throwable> handle5xxError(
            ClientResponse serverResponse) {
        int statusCode = serverResponse.statusCode().value();
        log.error("[GEMINI] Lỗi 5xx: status={}", statusCode);
        return reactor.core.publisher.Mono.error(
            new RetryableException("Server error: " + statusCode)
        );
    }

    private long handleRetryableError(int attempt, int maxAttempts, String reportId, 
                                      RetryableException e, long currentDelayMs) {
        if (attempt < maxAttempts) {
            log.warn("[GEMINI] Attempt {}/{} failed (retryable) cho reportId={}: {} - Retry sau {}ms", 
                    attempt, maxAttempts, reportId, e.getMessage(), currentDelayMs);
            sleep(currentDelayMs);
            return calculateNextDelay(currentDelayMs);
        } else {
            log.error("[GEMINI] Tất cả {} attempts thất bại cho reportId={}", maxAttempts, reportId);
            return currentDelayMs;
        }
    }

    private long handleTimeoutError(int attempt, int maxAttempts, String reportId, 
                                    IllegalStateException e, long currentDelayMs) {
        if (e.getCause() instanceof TimeoutException) {
            if (attempt < maxAttempts) {
                log.warn("[GEMINI] Attempt {}/{} timeout cho reportId={} - Retry sau {}ms",
                        attempt, maxAttempts, reportId, currentDelayMs);
                sleep(currentDelayMs);
                return calculateNextDelay(currentDelayMs);
            } else {
                log.error("[GEMINI] Timeout sau {} attempts cho reportId={}", maxAttempts, reportId);
                return currentDelayMs;
            }
        } else {
            log.error("[GEMINI] Non-retryable IllegalStateException cho reportId={}: {}", 
                    reportId, e.getMessage());
            throw new RuntimeException("Gemini API failed: " + e.getMessage(), e);
        }
    }

    private void handleNonRetryableError(String reportId, Exception e) {
        log.error("[GEMINI] Non-retryable error cho reportId={}: {}", reportId, e.getMessage());
        throw new RuntimeException("Gemini API failed: " + e.getMessage(), e);
    }

    private long calculateNextDelay(long currentDelayMs) {
        return Math.min(
            (long) (currentDelayMs * geminiProperties.getRetry().getMultiplier()),
            geminiProperties.getRetry().getMaxDelayMs()
        );
    }

    private RuntimeException buildMaxAttemptsException(int maxAttempts, Exception lastException) {
        String message = "Gemini API failed after " + maxAttempts + " attempts: " + 
                (lastException != null ? lastException.getMessage() : "unknown error");
        return new RuntimeException(message, lastException);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[GEMINI] Sleep interrupted");
        }
    }


    private static class RetryableException extends RuntimeException {
        public RetryableException(String message) {
            super(message);
        }
    }

    private String downloadAndEncodeImage(String imageUrl) {
        log.info("[GEMINI] Bắt đầu tải ảnh từ Firebase: {}", imageUrl);
        
        try {
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new IllegalArgumentException("Image URL is null or empty");
            }
            
            byte[] imageBytes = imageDownloadClient.get()
                    .uri(URI.create(imageUrl))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        log.error("[GEMINI] Lỗi 4xx khi tải ảnh: status={}, url={}", 
                                clientResponse.statusCode(), imageUrl);
                        return clientResponse.bodyToMono(String.class).flatMap(body -> {
                            log.error("[GEMINI] Response body: {}", body);
                            return reactor.core.publisher.Mono.error(
                                new RuntimeException("Image download failed with status: " + clientResponse.statusCode())
                            );
                        });
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, serverResponse -> {
                        log.error("[GEMINI] Lỗi 5xx khi tải ảnh: status={}, url={}", 
                                serverResponse.statusCode(), imageUrl);
                        return reactor.core.publisher.Mono.error(
                            new RuntimeException("Firebase server error: " + serverResponse.statusCode())
                        );
                    })
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (imageBytes == null || imageBytes.length == 0) {
                log.error("[GEMINI] Ảnh tải về rỗng hoặc null từ URL: {}", imageUrl);
                throw new RuntimeException("Downloaded image is empty or null");
            }
            
            if (imageBytes.length > 10 * 1024 * 1024) {
                log.error("[GEMINI] Ảnh quá lớn: {} bytes (max 10MB)", imageBytes.length);
                throw new RuntimeException("Image size exceeds 10MB limit");
            }
            
            log.info("[GEMINI] Tải ảnh thành công: {} bytes, đang encode Base64...", imageBytes.length);
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            log.info("[GEMINI] Encode Base64 hoàn tất: {} characters", base64Image.length());
            
            return base64Image;
            
        } catch (IllegalArgumentException e) {
            log.error("[GEMINI] Invalid image URL: {}", e.getMessage());
            throw new RuntimeException("Invalid image URL: " + e.getMessage(), e);
            
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            log.error("[GEMINI] Network error khi tải ảnh từ {}: {}", imageUrl, e.getMessage());
            throw new RuntimeException("Network error downloading image: " + e.getMessage(), e);
            
        } catch (Exception e) {
            log.error("[GEMINI] Lỗi không xác định khi tải/encode ảnh từ {}: {}", 
                    imageUrl, e.getMessage(), e);
            throw new RuntimeException("Cannot process image URL: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildRequestPayload(String base64Image, String severity, String description) {

        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))
        );

        Map<String, Object> imagePart = Map.of(
                "inlineData", Map.of(
                        "mimeType", "image/jpeg",
                        "data", base64Image
                )
        );

        String safeSeverity = (severity != null && !severity.isBlank()) ? severity : "Unknown";
        String safeDescription = (description != null && !description.isBlank()) ? description : "No description provided";

        String dynamicPrompt = String.format("""
                Analyze the attached image and cross-reference it with the user's report.
                - Claimed Severity Level by User: %s (Scale: LOW = ankle deep, MEDIUM = knee deep, HIGH = above knee, CRITICAL = catastrophic)
                - User's Description: "%s"
                
                PENALIZE FAKES: If the user claims HIGH/CRITICAL but the image shows a dry street, indoor setting, or a puddle, you MUST give a confidence_score of 0.
                """, safeSeverity, safeDescription);

        Map<String, Object> textPart = Map.of("text", dynamicPrompt);

        Map<String, Object> content = Map.of(
                "role", "user",
                "parts", List.of(imagePart, textPart)
        );

        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json"
        );

        return Map.of(
                "systemInstruction", systemInstruction,
                "contents", List.of(content),
                "generationConfig", generationConfig
        );
    }


    private GeminiVisionResponse parseGeminiResponse(String rawResponseBody) {
        validateResponseBody(rawResponseBody);

        try {
            log.debug("[GEMINI] Parsing response: {}", 
                    rawResponseBody.substring(0, Math.min(200, rawResponseBody.length())));
            
            JsonNode root = objectMapper.readTree(rawResponseBody);
            JsonNode candidates = extractCandidates(root, rawResponseBody);
            JsonNode firstCandidate = extractFirstCandidate(candidates);
            
            logFinishReason(firstCandidate);
            
            String generatedText = extractGeneratedText(firstCandidate, rawResponseBody);
            GeminiVisionResponse response = parseGeneratedText(generatedText);
            
            validateAndNormalizeConfidenceScore(response);
            
            return response;

        } catch (JsonProcessingException e) {
            log.error("[GEMINI] Lỗi parse JSON: {}", e.getMessage());
            log.error("[GEMINI] Raw response: {}", rawResponseBody);
            throw new RuntimeException("Cannot parse Gemini response as JSON: " + e.getMessage(), e);
            
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            log.error("[GEMINI] Cấu trúc response không đúng: {}", e.getMessage());
            log.error("[GEMINI] Raw response: {}", rawResponseBody);
            throw new RuntimeException("Gemini response structure is invalid: " + e.getMessage(), e);
        }
    }

    private void validateResponseBody(String rawResponseBody) {
        if (rawResponseBody == null || rawResponseBody.isBlank()) {
            log.error("[GEMINI] Response body rỗng hoặc null");
            throw new RuntimeException("Gemini API returned empty response");
        }
    }

    private JsonNode extractCandidates(JsonNode root, String rawResponseBody) {
        JsonNode candidates = root.path("candidates");
        if (candidates.isMissingNode() || candidates.isEmpty()) {
            log.error("[GEMINI] Response không có candidates. Full response: {}", rawResponseBody);
            throw new RuntimeException("Gemini response missing 'candidates' field");
        }
        return candidates;
    }

    private JsonNode extractFirstCandidate(JsonNode candidates) {
        JsonNode firstCandidate = candidates.get(0);
        if (firstCandidate == null) {
            log.error("[GEMINI] Candidate đầu tiên là null");
            throw new RuntimeException("First candidate is null");
        }
        return firstCandidate;
    }

    private void logFinishReason(JsonNode firstCandidate) {
        JsonNode finishReason = firstCandidate.path("finishReason");
        if (!finishReason.isMissingNode() && !"STOP".equals(finishReason.asText())) {
            log.warn("[GEMINI] Finish reason không phải STOP: {}", finishReason.asText());
        }
    }

    private String extractGeneratedText(JsonNode firstCandidate, String rawResponseBody) {
        JsonNode content = firstCandidate.path("content");
        if (content.isMissingNode()) {
            log.error("[GEMINI] Missing 'content' field in candidate");
            throw new RuntimeException("Candidate missing 'content' field");
        }
        
        JsonNode parts = content.path("parts");
        if (parts.isMissingNode() || parts.isEmpty()) {
            log.error("[GEMINI] Missing or empty 'parts' field");
            throw new RuntimeException("Content missing 'parts' field");
        }
        
        JsonNode firstPart = parts.get(0);
        if (firstPart == null) {
            log.error("[GEMINI] First part is null");
            throw new RuntimeException("First part is null");
        }
        
        String generatedText = firstPart.path("text").asText();
        if (generatedText == null || generatedText.isBlank()) {
            log.error("[GEMINI] Generated text is empty");
            throw new RuntimeException("Generated text is empty");
        }
        
        log.debug("[GEMINI] Generated text: {}", generatedText);
        return generatedText;
    }

    private GeminiVisionResponse parseGeneratedText(String generatedText) throws JsonProcessingException {
        return objectMapper.readValue(generatedText, GeminiVisionResponse.class);
    }

    private void validateAndNormalizeConfidenceScore(GeminiVisionResponse response) {
        int score = response.getConfidenceScore();
        if (score < 0 || score > 100) {
            log.warn("[GEMINI] Confidence score ngoài phạm vi [0-100]: {}", score);
            response.setConfidenceScore(Math.max(0, Math.min(100, score)));
        }
    }
}
