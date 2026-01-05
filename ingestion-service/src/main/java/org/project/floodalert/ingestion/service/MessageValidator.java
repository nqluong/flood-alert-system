package org.project.floodalert.ingestion.service;

import org.project.floodalert.ingestion.domain.ValidationResult;

public interface MessageValidator {
    ValidationResult validate(String payload);
}
