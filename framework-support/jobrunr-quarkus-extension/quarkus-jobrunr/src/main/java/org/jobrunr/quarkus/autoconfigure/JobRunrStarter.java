package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import org.jobrunr.storage.StorageProvider;


@Dependent
public class JobRunrStarter {

    JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration;
    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;

    Instance<StorageProvider> storageProviderInstance;

    public JobRunrStarter(JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration, JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration, Instance<StorageProvider> storageProviderInstance) {
        this.jobRunrBuildTimeConfiguration = jobRunrBuildTimeConfiguration;
        this.jobRunrRuntimeConfiguration = jobRunrRuntimeConfiguration;
        this.storageProviderInstance = storageProviderInstance;
    }

    void startup(@Observes StartupEvent event) {
        if (!jobRunrBuildTimeConfiguration.backgroundJobServer().included() && jobRunrRuntimeConfiguration.backgroundJobServer().enabled()) {
            throw new IllegalStateException("The BackgroundJobServer cannot be enabled, its resources were not included at build time. Please rebuild your project to include the required resources or disable the BackgroundJobServer.");
        }
        if (!jobRunrBuildTimeConfiguration.dashboard().included() && jobRunrRuntimeConfiguration.dashboard().enabled()) {
            throw new IllegalStateException("The JobRunrDashboardWebServer cannot be enabled, its resources were not included at build time. Please rebuild your project to include the required resources or disable the JobRunrDashboardWebServer.");
        }
    }

    void shutdown(@Observes ShutdownEvent event) {
        storageProviderInstance.get().close();
    }
}
