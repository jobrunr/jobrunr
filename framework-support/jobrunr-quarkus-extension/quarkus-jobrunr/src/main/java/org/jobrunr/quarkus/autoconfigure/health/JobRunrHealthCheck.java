package org.jobrunr.quarkus.autoconfigure.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;
import org.jobrunr.server.BackgroundJobServer;


@Readiness
@ApplicationScoped
public class JobRunrHealthCheck implements HealthCheck {

    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;

    Instance<BackgroundJobServer> backgroundJobServerInstance;

    public JobRunrHealthCheck(JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration, Instance<BackgroundJobServer> backgroundJobServerInstance) {
        this.jobRunrRuntimeConfiguration = jobRunrRuntimeConfiguration;
        this.backgroundJobServerInstance = backgroundJobServerInstance;
    }

    @Override
    public HealthCheckResponse call() {
        final HealthCheckResponseBuilder healthResponseBuilder = HealthCheckResponse.named("JobRunr");
        if (!jobRunrRuntimeConfiguration.backgroundJobServer().enabled()) {
            healthResponseBuilder
                    .up()
                    .withData("backgroundJobServer", "disabled");
        } else {
            final BackgroundJobServer backgroundJobServer = backgroundJobServerInstance.get();
            if (backgroundJobServer.isRunning()) {
                healthResponseBuilder
                        .up()
                        .withData("backgroundJobServer", "enabled")
                        .withData("backgroundJobServerStatus", "running");
            } else {
                healthResponseBuilder
                        .down()
                        .withData("backgroundJobServer", "enabled")
                        .withData("backgroundJobServerStatus", "stopped");
            }
        }
        return healthResponseBuilder.build();
    }
}
