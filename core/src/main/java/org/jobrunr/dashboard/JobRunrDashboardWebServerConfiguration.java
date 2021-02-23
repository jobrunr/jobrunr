package org.jobrunr.dashboard;

/**
 * This class allows to configure the JobRunrDashboard
 */
public class JobRunrDashboardWebServerConfiguration {
    int port = 8000;
    String path = "/dashboard";

    private JobRunrDashboardWebServerConfiguration() {

    }

    /**
     * This returns the default configuration with the JobRunrDashboard running on port 8000
     *
     * @return the default JobRunrDashboard configuration
     */
    public static JobRunrDashboardWebServerConfiguration usingStandardDashboardConfiguration() {
        return new JobRunrDashboardWebServerConfiguration();
    }

    /**
     * Specifies the port on which the JobRunrDashboard will run
     *
     * @param port the port on which the JobRunrDashboard will run
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrDashboardWebServerConfiguration andPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * Specifies the path where the JobRunrDashboard will listen
     *
     * @param path the path where the JobRunrDashboard will listen
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrDashboardWebServerConfiguration andPath(String path) {
        this.path = path;
        return this;
    }
}
