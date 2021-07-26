package org.jobrunr.autoconfigure.metric;

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
    @ConditionalOnBean({StorageProvider.class})
    public StorageProviderMetricsBinder storageProviderMetricsBinder(StorageProvider storageProvider) {
        return new StorageProviderMetricsBinder(storageProvider);
    }

    @Bean
    @ConditionalOnBean({BackgroundJobServer.class})
    public BackgroundJobServerMetricsBinder backgroundJobServerMetricsBinder(BackgroundJobServer backgroundJobServer) {
        return new BackgroundJobServerMetricsBinder(backgroundJobServer);
    }
}
