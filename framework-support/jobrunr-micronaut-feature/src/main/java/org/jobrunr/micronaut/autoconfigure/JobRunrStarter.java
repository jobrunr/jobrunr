package org.jobrunr.micronaut.autoconfigure;

import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerShutdownEvent;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;

import java.util.Optional;

@Singleton
public class JobRunrStarter {

    @Inject
    private JobRunrConfiguration configuration;

    @Inject
    private StorageProvider storageProvider;

    @Inject
    private Optional<BackgroundJobServer> backgroundJobServer;

    @Inject
    private Optional<JobRunrDashboardWebServer> dashboardWebServer;

    @EventListener
    void startup(ServerStartupEvent event) {
        if (configuration.getBackgroundJobServer().isEnabled()) {
            backgroundJobServer.get().start();
        }
        if (configuration.getDashboard().isEnabled()) {
            dashboardWebServer.get().start();
        }
    }

    @EventListener
    void shutdown(ServerShutdownEvent event) {
        if (configuration.getBackgroundJobServer().isEnabled()) {
            backgroundJobServer.get().stop();
        }
        if (configuration.getDashboard().isEnabled()) {
            dashboardWebServer.get().stop();
        }
        storageProvider.close();
    }
}
