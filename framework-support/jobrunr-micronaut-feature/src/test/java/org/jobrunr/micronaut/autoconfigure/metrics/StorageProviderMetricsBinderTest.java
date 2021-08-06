package org.jobrunr.micronaut.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;

@MicronautTest(rebuildContext = true)
class StorageProviderMetricsBinderTest {

    @Inject
    ApplicationContext context;

    @Test
    @Property(name = "jobrunr.background-job-server.enabled", value = "true")
    void backgroundJobServerEnabled() {
        assertThat(context).hasSingleBean(BackgroundJobServerMetricsBinder.class);
    }

    @Test
    @Property(name = "jobrunr.background-job-server.enabled", value = "false")
    void backgroundJobServerDisabled() {
        assertThat(context).doesNotHaveBean(BackgroundJobServerMetricsBinder.class);
    }

    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}