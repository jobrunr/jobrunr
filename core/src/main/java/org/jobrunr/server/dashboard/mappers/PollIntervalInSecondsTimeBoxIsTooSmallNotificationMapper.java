package org.jobrunr.server.dashboard.mappers;

import org.jobrunr.server.dashboard.DashboardNotification;
import org.jobrunr.server.dashboard.PollIntervalInSecondsTimeBoxIsTooSmallNotification;
import org.jobrunr.storage.JobRunrMetadata;

import java.util.UUID;

public class PollIntervalInSecondsTimeBoxIsTooSmallNotificationMapper implements DashboardNotificationMapper<PollIntervalInSecondsTimeBoxIsTooSmallNotification> {

    private final String id;

    public PollIntervalInSecondsTimeBoxIsTooSmallNotificationMapper(UUID backgroundJobServerId) {
        this.id = "BackgroundJobServer " + backgroundJobServerId;
    }

    @Override
    public boolean supports(DashboardNotification notification) {
        return notification instanceof PollIntervalInSecondsTimeBoxIsTooSmallNotification;
    }

    @Override
    public JobRunrMetadata mapToMetadata(PollIntervalInSecondsTimeBoxIsTooSmallNotification notification) {
        return new JobRunrMetadata(PollIntervalInSecondsTimeBoxIsTooSmallNotification.class.getSimpleName(), id, notification.getAmountOfSeconds());
    }
}
