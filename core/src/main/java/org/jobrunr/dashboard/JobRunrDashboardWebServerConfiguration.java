package org.jobrunr.dashboard;

public class JobRunrDashboardWebServerConfiguration {
    int port = 8000;

    JobRunrDashboardWebServerConfiguration() {

    }

    public static JobRunrDashboardWebServerConfiguration usingStandardConfiguration() {
        return new JobRunrDashboardWebServerConfiguration();
    }

    public JobRunrDashboardWebServerConfiguration andPort(int port) {
        this.port = port;
        return this;
    }
}
