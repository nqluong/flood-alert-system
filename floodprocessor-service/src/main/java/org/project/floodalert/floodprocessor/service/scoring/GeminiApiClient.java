package org.project.floodalert.floodprocessor.service.scoring;

import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.dto.response.GeminiVisionResponse;


public interface GeminiApiClient {

    GeminiVisionResponse analyzeFloodImage(ReportMessage msg);
}
