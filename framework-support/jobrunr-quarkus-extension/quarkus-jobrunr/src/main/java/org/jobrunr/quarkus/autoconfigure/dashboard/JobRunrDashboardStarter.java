package org.jobrunr.quarkus.autoconfigure.dashboard;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;

@Dependent
public class JobRunrDashboardStarter {

    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;

    Instance<JobRunrDashboardWebServer> dashboardWebServerInstance;

    public JobRunrDashboardStarter(JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration, Instance<JobRunrDashboardWebServer> dashboardWebServerInstance) {
        this.jobRunrRuntimeConfiguration = jobRunrRuntimeConfiguration;
        this.dashboardWebServerInstance = dashboardWebServerInstance;
    }

    void startup(@Observes StartupEvent event) {
        if (jobRunrRuntimeConfiguration.dashboard().enabled()) {
            dashboardWebServerInstance.get().start();
        }
    }

    void shutdown(@Observes ShutdownEvent event) {
        if (jobRunrRuntimeConfiguration.dashboard().enabled()) {
            dashboardWebServerInstance.get().stop();
        }
    }
}
