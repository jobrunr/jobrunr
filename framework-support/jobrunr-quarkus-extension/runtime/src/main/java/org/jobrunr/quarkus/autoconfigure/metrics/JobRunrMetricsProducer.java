package org.jobrunr.quarkus.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.arc.DefaultBean;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.metrics.BackgroundJobServerMetricsBinder;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

@Singleton
public class JobRunrMetricsProducer {

    public static class StorageProviderMetricsProducer {
        @Produces
        @DefaultBean
        @Singleton
        public StorageProviderMetricsBinder storageProviderMetricsBinder(StorageProvider storageProvider, MeterRegistry meterRegistry) {
            return new StorageProviderMetricsBinder(storageProvider, meterRegistry);
        }
    }

    public static class BackgroundJobServerMetricsProducer {
        @Produces
        @DefaultBean
        @Singleton
        public BackgroundJobServerMetricsBinder backgroundJobServerMetricsBinder(BackgroundJobServer backgroundJobServer, MeterRegistry meterRegistry) {
            return new BackgroundJobServerMetricsBinder(backgroundJobServer, meterRegistry);
        }
    }
}
