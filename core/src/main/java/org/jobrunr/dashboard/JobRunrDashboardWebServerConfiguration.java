package org.jobrunr.dashboard;

/**
 * This class allows to configure the JobRunrDashboard
 */
public class JobRunrDashboardWebServerConfiguration {
    int port = 8000;
    String username = null;
    String password = null;
    boolean allowAnonymousDataUsage = true;

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
     * Adds basic authentication to the dashboard using the provided username and password.
     * <span class="strong">WARNING</span> the password will be stored in clear text and if you are using http, it can be easily intercepted.
     *
     * @param username the login which the JobRunrDashboard will ask
     * @param password the password which the JobRunrDashboard will ask
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrDashboardWebServerConfiguration andBasicAuthentication(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }


    /**
     * Allows to opt-out of anonymous usage statistics. This setting is true by default and sends only the total amount of succeeded jobs processed
     * by your cluster per day to show a counter on the JobRunr website for marketing purposes.
     *
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrDashboardWebServerConfiguration andAllowAnonymousDataUsage(boolean allowAnonymousDataUsage) {
        this.allowAnonymousDataUsage = allowAnonymousDataUsage;
        return this;
    }
}
