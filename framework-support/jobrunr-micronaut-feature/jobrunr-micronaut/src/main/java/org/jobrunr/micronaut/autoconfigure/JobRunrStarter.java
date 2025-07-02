package org.jobrunr.micronaut.autoconfigure;

import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
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
    void startup(StartupEvent event) {
        if (configuration.getBackgroundJobServer().isEnabled()) {
            backgroundJobServer.get().start();
        }
        if (configuration.getDashboard().isEnabled()) {
            dashboardWebServer.get().start();
        }
    }

    @EventListener
    void shutdown(ShutdownEvent event) {
        if (configuration.getBackgroundJobServer().isEnabled()) {
            backgroundJobServer.get().stop();
        }
        if (configuration.getDashboard().isEnabled()) {
            dashboardWebServer.get().stop();
        }
        storageProvider.close();
    }
}
