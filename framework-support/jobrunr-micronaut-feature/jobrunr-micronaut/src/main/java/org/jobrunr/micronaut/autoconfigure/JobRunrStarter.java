package org.jobrunr.micronaut.autoconfigure;

import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;

import java.util.Optional;

@Singleton
public class JobRunrStarter {

    private final StorageProvider storageProvider;
    private final Optional<BackgroundJobServer> backgroundJobServer;
    private final Optional<JobRunrDashboardWebServer> dashboardWebServer;

    public JobRunrStarter(StorageProvider storageProvider, Optional<BackgroundJobServer> backgroundJobServer, Optional<JobRunrDashboardWebServer> dashboardWebServer) {
        this.storageProvider = storageProvider;
        this.backgroundJobServer = backgroundJobServer;
        this.dashboardWebServer = dashboardWebServer;
    }

    @EventListener
    void startup(StartupEvent event) {
        backgroundJobServer.ifPresent(BackgroundJobServer::start);
        dashboardWebServer.ifPresent(JobRunrDashboardWebServer::start);
    }

    @EventListener
    void shutdown(ShutdownEvent event) {
        backgroundJobServer.ifPresent(BackgroundJobServer::stop);
        dashboardWebServer.ifPresent(JobRunrDashboardWebServer::stop);
        storageProvider.close();
    }
}
