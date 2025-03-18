package org.jobrunr.quarkus.autoconfigure.dashboard;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;

import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;

@Singleton
public class JobRunrDashboardProducer {

    @Inject
    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;

    @Produces
    @DefaultBean
    @Singleton
    JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration() {
        if (jobRunrRuntimeConfiguration.dashboard().enabled()) {
            final JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration = usingStandardDashboardConfiguration();
            jobRunrRuntimeConfiguration.dashboard().port().ifPresent(dashboardWebServerConfiguration::andPort);
            if (jobRunrRuntimeConfiguration.dashboard().username().isPresent() && jobRunrRuntimeConfiguration.dashboard().password().isPresent()) {
                dashboardWebServerConfiguration.andBasicAuthentication(jobRunrRuntimeConfiguration.dashboard().username().get(), jobRunrRuntimeConfiguration.dashboard().password().get());
            }
            dashboardWebServerConfiguration.andAllowAnonymousDataUsage(jobRunrRuntimeConfiguration.miscellaneous().allowAnonymousDataUsage());
            return dashboardWebServerConfiguration;
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
    @LookupIfProperty(name = "quarkus.jobrunr.dashboard.enabled", stringValue = "true")
    JobRunrDashboardWebServer dashboardWebServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration) {
        if (jobRunrRuntimeConfiguration.dashboard().enabled()) {
            return new JobRunrDashboardWebServer(storageProvider, jobRunrJsonMapper, dashboardWebServerConfiguration);
        }
        return null;
    }
}
