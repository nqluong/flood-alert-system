package org.project.floodalert.floodprocessor.enums;

public enum SeverityLevel {
    NONE(0.0, 5.0),
    LOW(5.0, 15.0),
    MEDIUM(15.0, 30.0),
    HIGH(30.0, 50.0),
    CRITICAL(50.0, Double.MAX_VALUE);

    private final double minCm;
    private final double maxCm;

    SeverityLevel(double minCm, double maxCm) {
        this.minCm = minCm;
        this.maxCm = maxCm;
    }

    // 1. Hàm dịch từ số liệu Cảm biến IoT (cm) ra Mức độ
    public static SeverityLevel fromSensorCm(double waterLevelCm) {
        for (SeverityLevel level : values()) {
            if (waterLevelCm >= level.minCm && waterLevelCm < level.maxCm) {
                return level;
            }
        }
        return NONE;
    }

    // 2. Hàm dịch từ kết quả JSON của Gemini AI ra Mức độ
    public static SeverityLevel fromGeminiEstimate(String estimate) {
        if (estimate == null) return NONE;
        return switch (estimate.toUpperCase()) {
            case "ANKLE_DEEP" -> LOW;
            case "KNEE_DEEP" -> MEDIUM;
            case "ABOVE_KNEE" -> HIGH;
            default -> NONE;
        };
    }
}
