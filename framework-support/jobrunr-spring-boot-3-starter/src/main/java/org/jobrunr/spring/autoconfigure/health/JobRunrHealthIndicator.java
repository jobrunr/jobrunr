package org.jobrunr.spring.autoconfigure.health;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.spring.autoconfigure.JobRunrProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class JobRunrHealthIndicator implements HealthIndicator {

    private final ObjectProvider<BackgroundJobServer> backgroundJobServerProvider;
    private final JobRunrProperties jobRunrProperties;

    public JobRunrHealthIndicator(JobRunrProperties jobRunrProperties, ObjectProvider<BackgroundJobServer> backgroundJobServerProvider) {
        this.jobRunrProperties = jobRunrProperties;
        this.backgroundJobServerProvider = backgroundJobServerProvider;
    }

    @Override
    public Health health() {
        final Health.Builder health = Health.unknown();
        if (!jobRunrProperties.getBackgroundJobServer().isEnabled()) {
            health
                    .up()
                    .withDetail("backgroundJobServer", "disabled");
        } else {
            final BackgroundJobServer backgroundJobServer = backgroundJobServerProvider.getObject();
            if (backgroundJobServer.isRunning()) {
                health
                        .up()
                        .withDetail("backgroundJobServer", "enabled")
                        .withDetail("backgroundJobServerStatus", "running");
            } else {
                health
                        .down()
                        .withDetail("backgroundJobServer", "enabled")
                        .withDetail("backgroundJobServerStatus", "stopped");
            }
        }
        return health.build();
    }
}
