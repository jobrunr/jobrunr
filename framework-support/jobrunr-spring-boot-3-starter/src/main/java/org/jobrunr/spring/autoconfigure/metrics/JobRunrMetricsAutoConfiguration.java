package org.jobrunr.spring.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.metrics.BackgroundJobServerMetricsBinder;
import org.jobrunr.spring.autoconfigure.JobRunrAutoConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {JobRunrAutoConfiguration.class, MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@ConditionalOnClass(Metrics.class)
@ConditionalOnBean({MeterRegistry.class})
public class JobRunrMetricsAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "jobrunr.jobs.metrics", name = "enabled", havingValue = "true")
    public StorageProviderMetricsBinder storageProviderMetricsBinder(StorageProvider storageProvider, MeterRegistry meterRegistry) {
        return new StorageProviderMetricsBinder(storageProvider, meterRegistry);
    }

    @Bean
    @ConditionalOnBean({BackgroundJobServer.class})
    @ConditionalOnProperty(prefix = "jobrunr.background-job-server.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public BackgroundJobServerMetricsBinder backgroundJobServerMetricsBinder(BackgroundJobServer backgroundJobServer, MeterRegistry meterRegistry) {
        return new BackgroundJobServerMetricsBinder(backgroundJobServer, meterRegistry);
    }
}
