package org.jobrunr.spring.autoconfigure.metric;


import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.metrics.BackgroundJobServerMetricsBinder;
import org.jobrunr.spring.autoconfigure.JobRunrAutoConfiguration;
import org.jobrunr.spring.autoconfigure.metrics.JobRunrMetricsAutoConfiguration;
import org.jobrunr.spring.autoconfigure.storage.JobRunrSqlStorageAutoConfiguration;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class JobRunrMetricsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JobRunrAutoConfiguration.class,
                    JobRunrSqlStorageAutoConfiguration.class,
                    JobRunrMetricsAutoConfiguration.class
            ));

    @Test
    void metricsAreIgnoredIfLibraryIsNotPresent() {
        this.contextRunner
                .withClassLoader(new FilteredClassLoader(Metrics.class))
                .withUserConfiguration(InMemoryStorageProvider.class)
                .run((context) -> {
                    assertThat(context).doesNotHaveBean(StorageProviderMetricsBinder.class);
                    assertThat(context).doesNotHaveBean(BackgroundJobServerMetricsBinder.class);
                });
    }

    @Test
    void metricsAreIgnoredIfNoMeterRegistry() {
        this.contextRunner
                .withUserConfiguration(InMemoryStorageProvider.class)
                .run((context) -> {
                    assertThat(context).doesNotHaveBean(StorageProviderMetricsBinder.class);
                    assertThat(context).doesNotHaveBean(BackgroundJobServerMetricsBinder.class);
                });
    }

    @Test
    void backgroundJobServerMetricsAreAutoConfiguredAndStorageProviderMetricsAreDisabled() {
        this.contextRunner
                .withUserConfiguration(InMemoryStorageProvider.class)
                .withPropertyValues("jobrunr.background-job-server.enabled=true")
                .withBean(SimpleMeterRegistry.class)
                .run((context) -> {
                    assertThat(context).doesNotHaveBean(StorageProviderMetricsBinder.class);
                    assertThat(context).hasSingleBean(BackgroundJobServerMetricsBinder.class);
                });
    }

    @Test
    void backgroundJobServerMetricsBinderIsIgnoredIfBackgroundJobServerIsNotPresent() {
        this.contextRunner
                .withUserConfiguration(InMemoryStorageProvider.class)
                .withPropertyValues("jobrunr.background-job-server.enabled=false")
                .withBean(SimpleMeterRegistry.class)
                .run((context) -> {
                    assertThat(context).doesNotHaveBean(BackgroundJobServerMetricsBinder.class);
                });
    }

    @Test
    void storageProviderMetricsBinderAreAutoConfiguredIfJobsMetricsAreEnabled() {
        this.contextRunner
                .withUserConfiguration(InMemoryStorageProvider.class)
                .withPropertyValues("jobrunr.jobs.metrics.enabled=true")
                .withPropertyValues("jobrunr.background-job-server.enabled=true")
                .withBean(SimpleMeterRegistry.class)
                .run((context) -> {
                    assertThat(context).hasSingleBean(StorageProviderMetricsBinder.class);
                    assertThat(context).hasSingleBean(BackgroundJobServerMetricsBinder.class);
                });
    }

    @Test
    void backgroundJobServerMetricsBinderIsIgnoredIfBackgroundJobServerMetricsAreDisabled() {
        this.contextRunner
                .withUserConfiguration(InMemoryStorageProvider.class)
                .withPropertyValues("jobrunr.background-job-server.enabled=true")
                .withPropertyValues("jobrunr.background-job-server.metrics.enabled=false")
                .withBean(SimpleMeterRegistry.class)
                .run((context) -> {
                    assertThat(context).hasSingleBean(BackgroundJobServer.class);
                    assertThat(context).doesNotHaveBean(BackgroundJobServerMetricsBinder.class);
                });
    }
}
