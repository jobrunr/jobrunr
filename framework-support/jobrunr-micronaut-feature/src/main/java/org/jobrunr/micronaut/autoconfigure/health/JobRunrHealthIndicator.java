package org.jobrunr.micronaut.autoconfigure.health;

import io.micronaut.context.annotation.Requires;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.health.indicator.AbstractHealthIndicator;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.management.health.indicator.annotation.Readiness;
import jakarta.inject.Singleton;
import org.jobrunr.micronaut.autoconfigure.JobRunrConfiguration;
import org.jobrunr.server.BackgroundJobServer;

import java.util.HashMap;
import java.util.Map;

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
            healthStatus = new HealthStatus("OUT_OF_SERVICE");
            return new HashMap<String, String>() {{
                put("backgroundJobServer", "disabled");
            }};
        } else {
            if (backgroundJobServer.isRunning()) {
                healthStatus = HealthStatus.UP;
                return new HashMap<String, String>() {{
                    put("backgroundJobServer", "enabled");
                    put("backgroundJobServerStatus", "running");
                }};
            } else {
                healthStatus = HealthStatus.DOWN;
                return new HashMap<String, String>() {{
                    put("backgroundJobServer", "enabled");
                    put("backgroundJobServerStatus", "stopped");
                }};
            }
        }
    }
}
