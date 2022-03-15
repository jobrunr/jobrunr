package org.jobrunr.micronaut.autoconfigure.health;

import io.micronaut.context.annotation.Requires;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.health.indicator.AbstractHealthIndicator;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.management.health.indicator.annotation.Readiness;
import jakarta.inject.Singleton;
import org.jobrunr.micronaut.autoconfigure.JobRunrConfiguration;
import org.jobrunr.server.BackgroundJobServer;

import java.util.Map;

import static org.jobrunr.utils.CollectionUtils.mapOf;

@Singleton
@Requires(classes = {HealthIndicator.class}, beans = {BackgroundJobServer.class})
@Requires(property = "jobrunr.health.enabled", value = "true", defaultValue = "true")
@Readiness
public class JobRunrHealthIndicator extends AbstractHealthIndicator<Map<String, String>> {

    private final BackgroundJobServer backgroundJobServer;
    private final JobRunrConfiguration configuration;

    public JobRunrHealthIndicator(BackgroundJobServer backgroundJobServer, JobRunrConfiguration configuration) {
        this.backgroundJobServer = backgroundJobServer;
        this.configuration = configuration;
    }

    @Override
    protected String getName() {
        return "jobrunr";
    }

    @Override
    protected Map<String, String> getHealthInformation() {
        if (!configuration.getBackgroundJobServer().isEnabled()) {
            healthStatus = HealthStatus.UP;
            return mapOf("backgroundJobServer", "disabled");
        } else {
            if (backgroundJobServer.isRunning()) {
                healthStatus = HealthStatus.UP;
                return mapOf(
                        "backgroundJobServer", "enabled",
                        "backgroundJobServerStatus", "running"
                );
            } else {
                healthStatus = HealthStatus.DOWN;
                return mapOf(
                        "backgroundJobServer", "enabled",
                        "backgroundJobServerStatus", "stopped"
                );
            }
        }
    }
}
