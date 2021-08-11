package org.jobrunr.server.dashboard.mappers;

import org.jobrunr.server.dashboard.CpuAllocationIrregularityNotification;
import org.jobrunr.server.dashboard.DashboardNotification;
import org.jobrunr.storage.JobRunrMetadata;

import java.util.UUID;

public class CpuAllocationIrregularityNotificationMapper implements DashboardNotificationMapper<CpuAllocationIrregularityNotification> {

    private final String id;

    public CpuAllocationIrregularityNotificationMapper(UUID backgroundJobServerId) {
        this.id = "BackgroundJobServer " + backgroundJobServerId;
    }

    @Override
    public boolean supports(DashboardNotification notification) {
        return notification instanceof CpuAllocationIrregularityNotification;
    }

    @Override
    public JobRunrMetadata mapToMetadata(CpuAllocationIrregularityNotification notification) {
        return new JobRunrMetadata(CpuAllocationIrregularityNotification.class.getSimpleName(), id, notification.getAmountOfSeconds());
    }
}
