package org.project.floodalert.floodprocessor.service.scoring.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.config.GeminiProperties;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.dto.response.GeminiVisionResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiWebClientImplTest {

    @Mock
    private GeminiProperties geminiProperties;

    @Mock
    private GeminiProperties.RetryConfig retry;

    private ObjectMapper objectMapper;

    private GeminiWebClientImpl geminiWebClient;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        lenient().when(geminiProperties.getTimeoutSeconds()).thenReturn(30);
        lenient().when(geminiProperties.getBaseUrl()).thenReturn("https://gemini.googleapis.com");
        lenient().when(geminiProperties.getModel()).thenReturn("gemini-1.5-flash");
        lenient().when(geminiProperties.getApiKey()).thenReturn("test-api-key");
        lenient().when(geminiProperties.getRetry()).thenReturn(retry);

        lenient().when(retry.getMaxAttempts()).thenReturn(3);
        lenient().when(retry.getInitialDelayMs()).thenReturn(100L);
        lenient().when(retry.getMultiplier()).thenReturn(2.0);
        lenient().when(retry.getMaxDelayMs()).thenReturn(1000L);

        geminiWebClient = new GeminiWebClientImpl(geminiProperties, objectMapper);
    }

    @Test
    void buildApiPath_returnsCorrectPath() {
        String result = ReflectionTestUtils.invokeMethod(
                geminiWebClient,
                "buildApiPath"
        );

        assertThat(result)
                .contains("gemini-1.5-flash")
                .contains("test-api-key");
    }

    @Test
    void calculateNextDelay_normalDelay_returnsMultipliedDelay() {
        Long result = ReflectionTestUtils.invokeMethod(
                geminiWebClient,
                "calculateNextDelay",
                100L
        );

        assertThat(result).isEqualTo(200L);
    }

    @Test
    void calculateNextDelay_exceedsMaxDelay_returnsMaxDelay() {
        Long result = ReflectionTestUtils.invokeMethod(
                geminiWebClient,
                "calculateNextDelay",
                900L
        );

        assertThat(result).isEqualTo(1000L);
    }

    @Test
    void buildMaxAttemptsException_returnsRuntimeException() {
        RuntimeException cause = new RuntimeException("original");

        RuntimeException result = ReflectionTestUtils.invokeMethod(
                geminiWebClient,
                "buildMaxAttemptsException",
                3,
                cause
        );

        assertThat(result)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("3 attempts")
                .hasCause(cause);
    }

    @Test
    void validateResponseBody_null_throwsException() {
        assertThrows(
                RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        geminiWebClient,
                        "validateResponseBody",
                        (Object) null
                )
        );
    }

    @Test
    void validateResponseBody_blank_throwsException() {
        assertThrows(
                RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        geminiWebClient,
                        "validateResponseBody",
                        " "
                )
        );
    }

    @Test
    void validateResponseBody_valid_doesNotThrow() {
        ReflectionTestUtils.invokeMethod(
                geminiWebClient,
                "validateResponseBody",
                "{\"candidates\":[]}"
        );
    }

    @Test
    void parseGeminiResponse_validResponse_returnsParsedObject() {
        String response = """
                {
                  "candidates": [
                    {
                      "finishReason": "STOP",
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"is_flooded\\":true,\\"water_level_estimate\\":\\"KNEE_DEEP\\",\\"confidence_score\\":85,\\"reasoning\\":\\"Flood detected\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        GeminiVisionResponse result = ReflectionTestUtils.invokeMethod(
                geminiWebClient,
                "parseGeminiResponse",
                response
        );

        assertThat(result).isNotNull();
        assertThat(result.isFlooded()).isTrue();
        assertThat(result.getConfidenceScore()).isEqualTo(85);
    }

    @Test
    void parseGeminiResponse_confidenceAbove100_normalizesScore() {
        String response = """
                {
                  "candidates": [
                    {
                      "finishReason": "STOP",
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"is_flooded\\":true,\\"water_level_estimate\\":\\"KNEE_DEEP\\",\\"confidence_score\\":150,\\"reasoning\\":\\"Flood detected\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        GeminiVisionResponse result = ReflectionTestUtils.invokeMethod(
                geminiWebClient,
                "parseGeminiResponse",
                response
        );

        assertThat(result.getConfidenceScore()).isEqualTo(100);
    }

    @Test
    void parseGeminiResponse_confidenceBelow0_normalizesScore() {
        String response = """
            {
              "candidates": [
                {
                  "finishReason": "STOP",
                  "content": {
                    "parts": [
                      {
                        "text": "{\\"is_flooded\\":false,\\"water_level_estimate\\":\\"NONE\\",\\"confidence_score\\":-10,\\"reasoning\\":\\"No flood\\"}"
                      }
                    ]
                  }
                }
              ]
            }
            """;

        GeminiVisionResponse result = ReflectionTestUtils.invokeMethod(
                geminiWebClient,
                "parseGeminiResponse",
                response
        );

        assertThat(result.getConfidenceScore()).isEqualTo(0);
    }

    @Test
    void parseGeminiResponse_emptyResponse_throwsException() {
        assertThrows(
                RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        geminiWebClient,
                        "parseGeminiResponse",
                        ""
                )
        );
    }

    @Test
    void parseGeminiResponse_missingCandidates_throwsException() {
        String response = """
                {
                  "invalid": []
                }
                """;

        assertThrows(
                RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        geminiWebClient,
                        "parseGeminiResponse",
                        response
                )
        );
    }

    @Test
    void parseGeminiResponse_emptyCandidates_throwsException() {
        String response = """
                {
                  "candidates": []
                }
                """;

        assertThrows(
                RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        geminiWebClient,
                        "parseGeminiResponse",
                        response
                )
        );
    }

    @Test
    void parseGeminiResponse_missingContent_throwsException() {
        String response = """
                {
                  "candidates": [
                    {
                    }
                  ]
                }
                """;

        assertThrows(
                RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        geminiWebClient,
                        "parseGeminiResponse",
                        response
                )
        );
    }

    @Test
    void parseGeminiResponse_missingParts_throwsException() {
        String response = """
                {
                  "candidates": [
                    {
                      "content": {}
                    }
                  ]
                }
                """;

        assertThrows(
                RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        geminiWebClient,
                        "parseGeminiResponse",
                        response
                )
        );
    }

    @Test
    void parseGeminiResponse_emptyGeneratedText_throwsException() {
        String response = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": ""
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        assertThrows(
                RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        geminiWebClient,
                        "parseGeminiResponse",
                        response
                )
        );
    }

    @Test
    void parseGeminiResponse_invalidGeneratedJson_throwsException() {
        String response = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "invalid json"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        assertThrows(
                RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        geminiWebClient,
                        "parseGeminiResponse",
                        response
                )
        );
    }

    @Test
    void buildRequestPayload_validInputs_returnsPayload() {
        Map<String, Object> result = ReflectionTestUtils.invokeMethod(
                geminiWebClient,
                "buildRequestPayload",
                "base64-image",
                "HIGH",
                "Flood on street"
        );

        assertThat(result).isNotNull();
        assertThat(result).containsKeys(
                "systemInstruction",
                "contents",
                "generationConfig"
        );
    }

    @Test
    void buildRequestPayload_nullInputs_usesFallbackValues() {
        Map<String, Object> result = ReflectionTestUtils.invokeMethod(
                geminiWebClient,
                "buildRequestPayload",
                "base64-image",
                null,
                null
        );

        assertThat(result).isNotNull();
    }

    @Test
    void buildRequestPayload_blankInputs_usesFallbackValues() {
        Map<String, Object> result = ReflectionTestUtils.invokeMethod(
                geminiWebClient,
                "buildRequestPayload",
                "base64-image",
                " ",
                " "
        );

        assertThat(result).isNotNull();
    }

    @Test
    void sleep_doesNotThrow() {
        ReflectionTestUtils.invokeMethod(
                geminiWebClient,
                "sleep",
                1L
        );
    }

    @Test
    void analyzeFloodImage_nullImageUrl_throwsException() {
        ReportMessage msg = mock(ReportMessage.class);

        when(msg.getReportId()).thenReturn("report-001");
        when(msg.getImageUrl()).thenReturn(null);

        assertThrows(
                RuntimeException.class,
                () -> geminiWebClient.analyzeFloodImage(msg)
        );
    }

    @Test
    void analyzeFloodImage_blankImageUrl_throwsException() {
        ReportMessage msg = mock(ReportMessage.class);

        when(msg.getReportId()).thenReturn("report-001");
        when(msg.getImageUrl()).thenReturn(" ");

        assertThrows(
                RuntimeException.class,
                () -> geminiWebClient.analyzeFloodImage(msg)
        );
    }
}