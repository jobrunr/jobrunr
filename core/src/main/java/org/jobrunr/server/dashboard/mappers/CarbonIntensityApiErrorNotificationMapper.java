package org.jobrunr.server.dashboard.mappers;

import org.jobrunr.server.dashboard.CarbonIntensityApiErrorNotification;
import org.jobrunr.server.dashboard.CpuAllocationIrregularityNotification;
import org.jobrunr.server.dashboard.DashboardNotification;
import org.jobrunr.storage.JobRunrMetadata;

import java.util.UUID;

public class CarbonIntensityApiErrorNotificationMapper implements DashboardNotificationMapper<CarbonIntensityApiErrorNotification> {

    private final String id;

    public CarbonIntensityApiErrorNotificationMapper(UUID backgroundJobServerId) {
        this.id = "BackgroundJobServer " + backgroundJobServerId;
    }

    @Override
    public boolean supports(DashboardNotification notification) {
        return notification instanceof CarbonIntensityApiErrorNotification;
    }

    @Override
    public JobRunrMetadata mapToMetadata(CarbonIntensityApiErrorNotification notification) {
        return new JobRunrMetadata(CarbonIntensityApiErrorNotification.class.getSimpleName(), id, "error");
    }
}
