package org.jobrunr.quarkus.configuation;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DashboardConfiguration {

    /**
     * Enables the JobRunr dashboard.
     */
    @ConfigItem(defaultValue = "false")
    boolean enabled;

    /**
     * The port on which the Dashboard should run
     */
    @ConfigItem(defaultValue = "8000")
    int port;

    /**
     * The username for the basic authentication which protects the dashboard
     */
    @ConfigItem
    String username;

    /**
     * The password for the basic authentication which protects the dashboard. WARNING: this is insecure as it is in clear text
     */
    @ConfigItem
    String password;
}
