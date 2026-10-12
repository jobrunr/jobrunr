package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.arc.DefaultBean;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;


@Dependent
@DefaultBean
public class JobRunrStarter {

    JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration;
    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;

    Instance<BackgroundJobServer> backgroundJobServerInstance;
    Instance<JobRunrDashboardWebServer> dashboardWebServerInstance;
    Instance<StorageProvider> storageProviderInstance;

    public JobRunrStarter(JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration, JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration, Instance<BackgroundJobServer> backgroundJobServerInstance, Instance<JobRunrDashboardWebServer> dashboardWebServerInstance, Instance<StorageProvider> storageProviderInstance) {
        this.jobRunrBuildTimeConfiguration = jobRunrBuildTimeConfiguration;
        this.jobRunrRuntimeConfiguration = jobRunrRuntimeConfiguration;
        this.backgroundJobServerInstance = backgroundJobServerInstance;
        this.dashboardWebServerInstance = dashboardWebServerInstance;
        this.storageProviderInstance = storageProviderInstance;
    }

    protected void startup(@Observes StartupEvent event) {
        if (!jobRunrBuildTimeConfiguration.backgroundJobServer().included() && jobRunrRuntimeConfiguration.backgroundJobServer().enabled()) {
            throw new IllegalStateException("The BackgroundJobServer cannot be enabled, its resources were not included at build time. Please rebuild your project to include the required resources or disable the BackgroundJobServer.");
        } else if (jobRunrRuntimeConfiguration.backgroundJobServer().enabled()) {
            backgroundJobServerInstance.get().start();
        }
        if (!jobRunrBuildTimeConfiguration.dashboard().included() && jobRunrRuntimeConfiguration.dashboard().enabled()) {
            throw new IllegalStateException("The JobRunrDashboardWebServer cannot be enabled, its resources were not included at build time. Please rebuild your project to include the required resources or disable the JobRunrDashboardWebServer.");
        } else if (jobRunrRuntimeConfiguration.dashboard().enabled()) {
            dashboardWebServerInstance.get().start();
        }
    }

    protected void shutdown(@Observes ShutdownEvent event) {
        if (backgroundJobServerInstance.isResolvable() && jobRunrRuntimeConfiguration.backgroundJobServer().enabled()) {
            backgroundJobServerInstance.get().stop();
        }
        if (dashboardWebServerInstance.isResolvable() && jobRunrRuntimeConfiguration.dashboard().enabled()) {
            dashboardWebServerInstance.get().stop();
        }
        storageProviderInstance.get().close();
    }
}
