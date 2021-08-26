package org.jobrunr.server.dashboard.mappers;

import org.jobrunr.server.dashboard.DashboardNotification;
import org.jobrunr.server.dashboard.NewJobRunrVersionNotification;
import org.jobrunr.storage.JobRunrMetadata;

public class NewJobRunrVersionNotificationMapper implements DashboardNotificationMapper<NewJobRunrVersionNotification> {

    @Override
    public boolean supports(DashboardNotification notification) {
        return notification instanceof NewJobRunrVersionNotification;
    }

    @Override
    public JobRunrMetadata mapToMetadata(NewJobRunrVersionNotification notification) {
        return new JobRunrMetadata(NewJobRunrVersionNotification.class.getSimpleName(), "cluster", notification.getLatestVersion());
    }
}
