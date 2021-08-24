package org.jobrunr.quarkus.autoconfigure.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.jobrunr.quarkus.autoconfigure.JobRunrConfiguration;
import org.jobrunr.server.BackgroundJobServer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;

@Readiness
@ApplicationScoped
public class JobRunrHealthCheck implements HealthCheck {

    JobRunrConfiguration configuration;

    Instance<BackgroundJobServer> backgroundJobServerInstance;

    public JobRunrHealthCheck(JobRunrConfiguration configuration, Instance<BackgroundJobServer> backgroundJobServerInstance) {
        this.configuration = configuration;
        this.backgroundJobServerInstance = backgroundJobServerInstance;
    }

    @Override
    public HealthCheckResponse call() {
        final HealthCheckResponseBuilder healthResponseBuilder = HealthCheckResponse.named("JobRunr");
        if (!configuration.backgroundJobServer.enabled) {
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
