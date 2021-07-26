package org.jobrunr.autoconfigure.metric;


import io.micrometer.core.instrument.Metrics;
import org.jobrunr.autoconfigure.JobRunrAutoConfiguration;
import org.jobrunr.autoconfigure.storage.JobRunrSqlStorageAutoConfiguration;
import org.jobrunr.metric.BackgroundJobServerMetricsBinder;
import org.jobrunr.metric.StorageProviderMetricsBinder;
import org.jobrunr.storage.InMemoryStorageProvider;
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
    void storageProviderMetricsIsIgnoredIfLibraryIsNotPresent() {
        this.contextRunner
                .withClassLoader(new FilteredClassLoader(Metrics.class))
                .withUserConfiguration(InMemoryStorageProvider.class)
                .run((context) -> {
                    assertThat(context).doesNotHaveBean(StorageProviderMetricsBinder.class);
                    assertThat(context).doesNotHaveBean(BackgroundJobServerMetricsBinder.class);
                });
    }

    @Test
    void metricsBinderIsIgnoredIfBackgroundJobServerIsNotPresent() {
        this.contextRunner
                .withUserConfiguration(InMemoryStorageProvider.class)
                .run((context) -> {
                    assertThat(context).hasSingleBean(StorageProviderMetricsBinder.class);
                    assertThat(context).doesNotHaveBean(BackgroundJobServerMetricsBinder.class);
                });
    }
}
