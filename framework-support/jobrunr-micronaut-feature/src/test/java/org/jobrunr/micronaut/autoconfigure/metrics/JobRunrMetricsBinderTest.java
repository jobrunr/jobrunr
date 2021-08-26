package org.jobrunr.micronaut.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jobrunr.server.metrics.BackgroundJobServerMetricsBinder;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;
import org.junit.jupiter.api.Test;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;

@MicronautTest(rebuildContext = true)
class JobRunrMetricsBinderTest {

    @Inject
    ApplicationContext context;

    @Test
    void storageProviderMetricsAreAutoConfigured() {
        assertThat(context).hasSingleBean(StorageProviderMetricsBinder.class);
    }

    @Test
    @Property(name = "jobrunr.background-job-server.enabled", value = "true")
    void backgroundJobServerMetricsAreAutoConfiguredIfBackgroundJobServerIsEnabled() {
        assertThat(context).hasSingleBean(BackgroundJobServerMetricsBinder.class);
    }

    @Test
    @Property(name = "jobrunr.background-job-server.enabled", value = "false")
    void backgroundJobServerMetricsAreDisabledIfBackgroundJobServerIsDisabled() {
        assertThat(context).doesNotHaveBean(BackgroundJobServerMetricsBinder.class);
    }

    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}