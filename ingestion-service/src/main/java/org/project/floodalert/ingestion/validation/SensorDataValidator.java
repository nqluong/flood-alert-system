package org.project.floodalert.ingestion.validation;

public interface SensorDataValidator {

    boolean validate(ValidationContext context);

    String getName();
}
