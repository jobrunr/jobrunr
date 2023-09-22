package org.jobrunr.quarkus.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.quarkus.autoconfigure.JobRunrConfiguration;
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
        JobRunrConfiguration configuration;

        @Produces
        @DefaultBean
        @Singleton
        public StorageProviderMetricsBinder storageProviderMetricsBinder(StorageProvider storageProvider, MeterRegistry meterRegistry) {
            if (configuration.jobs.metrics.enabled) {
                return new StorageProviderMetricsBinder(storageProvider, meterRegistry);
            }
            return null;
        }
    }

    public static class BackgroundJobServerMetricsProducer {
        @Inject
        JobRunrConfiguration configuration;

        @Produces
        @DefaultBean
        @Singleton
        public BackgroundJobServerMetricsBinder backgroundJobServerMetricsBinder(BackgroundJobServer backgroundJobServer, MeterRegistry meterRegistry) {
            if (configuration.backgroundJobServer.metrics.enabled) {
                return new BackgroundJobServerMetricsBinder(backgroundJobServer, meterRegistry);
            }
            return null;
        }
    }
}
