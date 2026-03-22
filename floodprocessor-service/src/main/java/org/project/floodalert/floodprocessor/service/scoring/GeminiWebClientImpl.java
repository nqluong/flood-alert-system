package org.project.floodalert.floodprocessor.service.scoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.config.GeminiProperties;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.dto.response.GeminiVisionResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

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

        this.geminiWebClient = WebClient.builder()
                .baseUrl(geminiProperties.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.imageDownloadClient = WebClient.builder().build();
    }

    @Override
    public GeminiVisionResponse analyzeFloodImage(ReportMessage msg) {

        // Xây dựng payload theo cấu trúc Gemini GenerateContent API
        String base64Image = downloadAndEncodeImage(msg.getImageUrl());
        // Xây dựng payload với Base64 và Lời khai của user
        Map<String, Object> requestPayload = buildRequestPayload(
                base64Image,
                msg.getSeverityLevel(),
                msg.getDescription()
        );

        String apiPath = "/v1beta/models/" + geminiProperties.getModel()
                + ":generateContent?key=" + geminiProperties.getApiKey();

        // Gọi Google API
        String responseBody = geminiWebClient.post()
                .uri(apiPath)
                .bodyValue(requestPayload)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class).map(body -> {
                            log.error("[GEMINI] Lỗi 4xx: status={}, body={}", clientResponse.statusCode(), body);
                            return new RuntimeException("Gemini API client error: " + clientResponse.statusCode());
                        })
                )
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(String.class).map(body -> {
                            log.error("[GEMINI] Lỗi 5xx: status={}, body={}", clientResponse.statusCode(), body);
                            return new RuntimeException("Gemini API server error: " + clientResponse.statusCode());
                        })
                )
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(geminiProperties.getTimeoutSeconds()))
                .block();

        return parseGeminiResponse(responseBody);
    }

    private String downloadAndEncodeImage(String imageUrl) {
        try {
            byte[] imageBytes = imageDownloadClient.get()
                    .uri(imageUrl)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(5)) // Giới hạn tải ảnh trong 5s
                    .block();

            if (imageBytes == null || imageBytes.length == 0) {
                throw new RuntimeException("Tải ảnh thất bại hoặc ảnh rỗng");
            }
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            log.error("Lỗi khi tải ảnh từ Firebase: {}", e.getMessage());
            throw new RuntimeException("Không thể xử lý URL ảnh", e);
        }
    }

    private Map<String, Object> buildRequestPayload(String base64Image, String severity, String description) {

        // Cấu hình luật cho AI
        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))
        );

        // Chuỗi Base64
        Map<String, Object> imagePart = Map.of(
                "inlineData", Map.of(  // Sử dụng camelCase theo chuẩn Google API
                        "mimeType", "image/jpeg",
                        "data", base64Image
                )
        );

        // Lời khai: Xử lý giá trị null và format String
        String safeSeverity = (severity != null && !severity.isBlank()) ? severity : "Unknown";
        String safeDescription = (description != null && !description.isBlank()) ? description : "No description provided";

        String dynamicPrompt = String.format("""
                Analyze the attached image and cross-reference it with the user's report.
                - Claimed Severity Level by User: %s (Scale: LOW = ankle deep, MEDIUM = knee deep, HIGH = above knee, CRITICAL = catastrophic)
                - User's Description: "%s"
                
                PENALIZE FAKES: If the user claims HIGH/CRITICAL but the image shows a dry street, indoor setting, or a puddle, you MUST give a confidence_score of 0.
                """, safeSeverity, safeDescription);

        Map<String, Object> textPart = Map.of("text", dynamicPrompt);

        // Gom lại gửi lên (Ảnh gửi trước, Text hướng dẫn gửi sau)
        Map<String, Object> content = Map.of(
                "role", "user",
                "parts", List.of(imagePart, textPart)
        );

        // Ép AI trả về JSON
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
        if (rawResponseBody == null || rawResponseBody.isBlank()) {
            throw new RuntimeException("Gemini API trả về response rỗng");
        }

        try {
            // Điều hướng qua cây JSON để lấy text do model sinh ra
            JsonNode root = objectMapper.readTree(rawResponseBody);
            String generatedText = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();

            return objectMapper.readValue(generatedText, GeminiVisionResponse.class);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Không thể parse Gemini response", e);
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            throw new RuntimeException("Cấu trúc Gemini response không như mong đợi", e);
        }
    }
}
