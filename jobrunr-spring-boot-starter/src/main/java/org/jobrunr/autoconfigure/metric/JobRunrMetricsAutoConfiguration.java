package org.jobrunr.autoconfigure.metric;

import io.micrometer.core.instrument.MeterRegistry;
import org.jobrunr.autoconfigure.JobRunrAutoConfiguration;
import org.jobrunr.metric.BackgroundJobServerMetricsBinder;
import org.jobrunr.metric.StorageProviderMetricsBinder;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(
        name = {"io.micrometer.core.instrument.Metrics"}
)
@AutoConfigureAfter({JobRunrAutoConfiguration.class})
public class JobRunrMetricsAutoConfiguration {

    @Bean
    @ConditionalOnBean({StorageProvider.class, MeterRegistry.class})
    public StorageProviderMetricsBinder storageProviderMetricsBinder(StorageProvider storageProvider, MeterRegistry meterRegistry) {
        return new StorageProviderMetricsBinder(storageProvider, meterRegistry);
    }

    @Bean
    @ConditionalOnBean({BackgroundJobServer.class, MeterRegistry.class})
    public BackgroundJobServerMetricsBinder backgroundJobServerMetricsBinder(BackgroundJobServer backgroundJobServer, MeterRegistry meterRegistry) {
        return new BackgroundJobServerMetricsBinder(backgroundJobServer, meterRegistry);
    }
}
