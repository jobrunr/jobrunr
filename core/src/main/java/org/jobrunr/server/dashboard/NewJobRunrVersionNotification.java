package org.jobrunr.server.dashboard;

import org.jobrunr.storage.JobRunrMetadata;

public class NewJobRunrVersionNotification implements DashboardNotification {

    private String latestVersion;

    public NewJobRunrVersionNotification(JobRunrMetadata metadata) {
        if (!metadata.getName().equals(NewJobRunrVersionNotification.class.getSimpleName())) {
            throw new IllegalStateException("Can only be constructed for JobRunrMetadata with key " + NewJobRunrVersionNotification.class.getSimpleName());
        }
        this.latestVersion = metadata.getValue();
    }

    public NewJobRunrVersionNotification(String latestVersion) {
        this.latestVersion = latestVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
