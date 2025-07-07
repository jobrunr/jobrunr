package org.jobrunr.quarkus.autoconfigure.metrics;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jobrunr.server.metrics.BackgroundJobServerMetricsBinder;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestProfile(JobRunrMetricsDisabledConfigurationTest.class)
public class JobRunrMetricsDisabledConfigurationTest implements QuarkusTestProfile {
    @Inject
    Instance<BackgroundJobServerMetricsBinder> backgroundJobServerMetricsBinderInstance;
    @Inject
    Instance<StorageProviderMetricsBinder> storageProviderMetricsBinderInstance;

    @Test
    void metricsBeansAreResolvable() {
        assertThat(backgroundJobServerMetricsBinderInstance.isUnsatisfied()).isTrue();

        assertThat(storageProviderMetricsBinderInstance.isUnsatisfied()).isTrue();
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.jobrunr.background-job-server.enabled", "true",
                "quarkus.jobrunr.background-job-server.metrics.enabled", "false",
                "quarkus.jobrunr.jobs.metrics.enabled", "false"
        );
    }
}
