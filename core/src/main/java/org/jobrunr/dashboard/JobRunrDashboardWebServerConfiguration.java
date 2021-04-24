package org.jobrunr.dashboard;

/**
 * This class allows to configure the JobRunrDashboard
 */
public class JobRunrDashboardWebServerConfiguration {
    int port = 8000;
    String login = null;
    String password = null;

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
     * Specifies the login which the JobRunrDashboard will ask
     *
     * @param login the login which the JobRunrDashboard will ask
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrDashboardWebServerConfiguration andLogin(String login) {
        this.login = login;
        return this;
    }

    /**
     * Specifies the password on which the JobRunrDashboard will ask
     *
     * @param password the password which the JobRunrDashboard will ask
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrDashboardWebServerConfiguration andPassword(String password) {
        this.password = password;
        return this;
    }
}
