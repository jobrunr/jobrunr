package org.jobrunr.server.dashboard.mappers;

import org.jobrunr.server.dashboard.DashboardNotification;
import org.jobrunr.storage.JobRunrMetadata;

public interface DashboardNotificationMapper<T extends DashboardNotification> {

    boolean supports(DashboardNotification notification);

    default JobRunrMetadata map(DashboardNotification notification) {
        return mapToMetadata((T) notification);
    }

    JobRunrMetadata mapToMetadata(T notification);

}
