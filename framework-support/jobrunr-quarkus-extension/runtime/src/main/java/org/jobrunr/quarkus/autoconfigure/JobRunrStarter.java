package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;


@Dependent
public class JobRunrStarter {

    JobRunrConfiguration configuration;

    Instance<BackgroundJobServer> backgroundJobServerInstance;

    Instance<JobRunrDashboardWebServer> dashboardWebServerInstance;

    Instance<StorageProvider> storageProviderInstance;

    public JobRunrStarter(JobRunrConfiguration configuration, Instance<BackgroundJobServer> backgroundJobServerInstance, Instance<JobRunrDashboardWebServer> dashboardWebServerInstance, Instance<StorageProvider> storageProviderInstance) {
        this.configuration = configuration;
        this.backgroundJobServerInstance = backgroundJobServerInstance;
        this.dashboardWebServerInstance = dashboardWebServerInstance;
        this.storageProviderInstance = storageProviderInstance;
    }

    void startup(@Observes StartupEvent event) {
        if (configuration.backgroundJobServer.enabled) {
            backgroundJobServerInstance.get().start();
        }
        if (configuration.dashboard.enabled) {
            dashboardWebServerInstance.get().start();
        }
    }

    void shutdown(@Observes ShutdownEvent event) {
        if (configuration.backgroundJobServer.enabled) {
            backgroundJobServerInstance.get().stop();
        }
        if (configuration.dashboard.enabled) {
            dashboardWebServerInstance.get().stop();
        }
        storageProviderInstance.get().close();
    }
}
