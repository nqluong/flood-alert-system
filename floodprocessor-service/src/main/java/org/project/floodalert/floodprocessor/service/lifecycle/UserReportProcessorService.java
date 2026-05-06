package org.project.floodalert.floodprocessor.service.lifecycle;


public interface UserReportProcessorService {

    void cleanupPendingReports();

    void applyTimeDecayAndSpatialCheck();
}
