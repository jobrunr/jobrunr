package org.jobrunr.quarkus.autoconfigure.server;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;
import org.jobrunr.server.BackgroundJobServer;

@Dependent
public class JobRunrBackgroundJobServerStarter {

    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;

    Instance<BackgroundJobServer> backgroundJobServerInstance;

    public JobRunrBackgroundJobServerStarter(JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration, Instance<BackgroundJobServer> backgroundJobServerInstance) {
        this.jobRunrRuntimeConfiguration = jobRunrRuntimeConfiguration;
        this.backgroundJobServerInstance = backgroundJobServerInstance;
    }

    void startup(@Observes StartupEvent event) {
        if (jobRunrRuntimeConfiguration.backgroundJobServer().enabled()) {
            backgroundJobServerInstance.get().start();
        }
    }

    void shutdown(@Observes ShutdownEvent event) {
        if (jobRunrRuntimeConfiguration.backgroundJobServer().enabled()) {
            backgroundJobServerInstance.get().stop();
        }
    }
}
