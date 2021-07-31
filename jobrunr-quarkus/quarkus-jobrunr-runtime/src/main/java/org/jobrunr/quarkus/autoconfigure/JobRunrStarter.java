package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;


@Dependent
public class JobRunrStarter {

    @Inject
    JobRunrConfiguration configuration;

    void startup(@Observes StartupEvent event) {
        if (configuration.backgroundJobServer.enabled) {
            final BackgroundJobServer backgroundJobServer = getBean(BackgroundJobServer.class).get();
            backgroundJobServer.start();
        }
        if (configuration.dashboard.enabled) {
            final JobRunrDashboardWebServer dashboardWebServer = getBean(JobRunrDashboardWebServer.class).get();
            dashboardWebServer.start();
        }
    }

    void shutdown(@Observes ShutdownEvent event) {
        if (configuration.backgroundJobServer.enabled) {
            final BackgroundJobServer backgroundJobServer = getBean(BackgroundJobServer.class).get();
            backgroundJobServer.stop();
        }
        if (configuration.dashboard.enabled) {
            final JobRunrDashboardWebServer dashboardWebServer = getBean(JobRunrDashboardWebServer.class).get();
            dashboardWebServer.stop();
        }
        final StorageProvider storageProvider = getBean(StorageProvider.class).get();
        storageProvider.close();
    }

    static <T> Instance<T> getBean(Class<T> clazz) {
        return CDI.current().select(clazz);
    }
}
