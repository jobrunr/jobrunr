package org.jobrunr.micronaut.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.metrics.BackgroundJobServerMetricsBinder;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;

@MicronautTest(rebuildContext = true)
@Property(name = "jobrunr.database.type", value = "mem")
@TestMethodOrder(MethodOrderer.MethodName.class)
class JobRunrMetricsBinderTest {

    @Inject
    ApplicationContext context;

    @Test
    void aFirstTestThatReloadsTheContextToMakeFlakyTestWork() {
        assertThat(context).hasSingleBean(StorageProvider.class);
    }

    @Test
    void storageProviderMetricsAreDisabledByDefault() {
        assertThat(context).doesNotHaveBean(BackgroundJobServerMetricsBinder.class);
    }

    @Test
    @Property(name = "jobrunr.jobs.metrics.enabled", value = "true")
    void storageProviderMetricsAreEnabledIfJobsMetricsAreEnabled() {
        assertThat(context).hasSingleBean(StorageProviderMetricsBinder.class);
    }

    @Test
    @Property(name = "jobrunr.background-job-server.enabled", value = "true")
    void backgroundJobServerMetricsAreAutoConfiguredIfBackgroundJobServerIsEnabled() {
        assertThat(context).hasSingleBean(BackgroundJobServer.class);
        assertThat(context).hasSingleBean(BackgroundJobServerMetricsBinder.class);
    }

    @Test
    @Property(name = "jobrunr.background-job-server.enabled", value = "false")
    void backgroundJobServerMetricsAreDisabledIfBackgroundJobServerIsDisabled() {
        assertThat(context).doesNotHaveBean(BackgroundJobServerMetricsBinder.class);
    }

    @Test
    @Property(name = "jobrunr.background-job-server.enabled", value = "true")
    @Property(name = "jobrunr.background-job-server.metrics.enabled", value = "false")
    void backgroundJobServerMetricsAreDisabledIfBackgroundJobServerMetricsAreDisabled() {
        assertThat(context).hasSingleBean(BackgroundJobServer.class);
        assertThat(context).doesNotHaveBean(BackgroundJobServerMetricsBinder.class);
    }

    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}