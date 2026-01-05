package org.project.floodalert.ingestion.domain;

import lombok.Value;

@Value
public class ValidationResult {
    boolean valid;
    String errorMessage;

    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }
    public static ValidationResult failure(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }
}
