package org.jobrunr.quarkus.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.quarkus.autoconfigure.JobRunrBuildTimeConfiguration;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.metrics.BackgroundJobServerMetricsBinder;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;

@Singleton
public class JobRunrMetricsProducer {

    private JobRunrMetricsProducer() {
    }

    public static class StorageProviderMetricsProducer {
        @Inject
        JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration;

        @Produces
        @DefaultBean
        @Singleton
        public StorageProviderMetricsBinder storageProviderMetricsBinder(StorageProvider storageProvider, MeterRegistry meterRegistry) {
            if (jobRunrBuildTimeConfiguration.jobs().metrics().enabled()) {
                return new StorageProviderMetricsBinder(storageProvider, meterRegistry);
            }
            return null;
        }
    }

    public static class BackgroundJobServerMetricsProducer {
        @Inject
        JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration;

        @Produces
        @DefaultBean
        @Singleton
        public BackgroundJobServerMetricsBinder backgroundJobServerMetricsBinder(BackgroundJobServer backgroundJobServer, MeterRegistry meterRegistry) {
            if (jobRunrBuildTimeConfiguration.backgroundJobServer().metrics().enabled()) {
                return new BackgroundJobServerMetricsBinder(backgroundJobServer, meterRegistry);
            }
            return null;
        }
    }
}
