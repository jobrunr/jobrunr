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
        name = {"io.micrometer.core.instrument.MeterRegistry"}
)
@AutoConfigureAfter({JobRunrAutoConfiguration.class})
public class JobRunrMetricsAutoConfiguration {

    @Bean
    @ConditionalOnBean({StorageProvider.class})
    public StorageProviderMetricsBinder storageProviderMetricsBinder(MeterRegistry registry, StorageProvider storageProvider) {
        return new StorageProviderMetricsBinder(registry, storageProvider);
    }

    @Bean
    @ConditionalOnBean({BackgroundJobServer.class})
    public BackgroundJobServerMetricsBinder backgroundJobServerMetricsBinder(MeterRegistry registry, BackgroundJobServer backgroundJobServer) {
        return new BackgroundJobServerMetricsBinder(registry, backgroundJobServer);
    }
}
