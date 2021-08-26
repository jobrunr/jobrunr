package org.jobrunr.server.dashboard;

public class NewJobRunrVersionNotification implements DashboardNotification {

    private String latestVersion;

    public NewJobRunrVersionNotification(String latestVersion) {
        this.latestVersion = latestVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
