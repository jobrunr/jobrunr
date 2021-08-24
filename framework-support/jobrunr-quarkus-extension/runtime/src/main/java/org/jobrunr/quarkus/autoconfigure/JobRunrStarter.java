package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.metrics.BackgroundJobServerMetricsBinder;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;


@Dependent
public class JobRunrStarter {

    @Inject
    JobRunrConfiguration configuration;

    @Inject
    Instance<BackgroundJobServer> backgroundJobServerInstance;

    @Inject
    Instance<JobRunrDashboardWebServer> dashboardWebServerInstance;

    @Inject
    Instance<StorageProvider> storageProviderInstance;

    @Inject
    Instance<StorageProviderMetricsBinder> storageProviderMetricsBinderInstance;

    @Inject
    Instance<BackgroundJobServerMetricsBinder> backgroundJobServerMetricsBinderInstance;

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
