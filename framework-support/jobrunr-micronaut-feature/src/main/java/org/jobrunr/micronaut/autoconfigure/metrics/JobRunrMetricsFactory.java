package org.jobrunr.micronaut.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.metrics.BackgroundJobServerMetricsBinder;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;

@Factory
@Requires(classes = MeterRegistry.class)
public class JobRunrMetricsFactory {

    @Singleton
    @Requires(beans = StorageProvider.class)
    public StorageProviderMetricsBinder storageProviderMetricsBinder(StorageProvider storageProvider, MeterRegistry meterRegistry) {
        return new StorageProviderMetricsBinder(storageProvider, meterRegistry);
    }

    @Singleton
    @Requires(beans = BackgroundJobServer.class)
    public BackgroundJobServerMetricsBinder backgroundJobServerMetricsBinder(BackgroundJobServer backgroundJobServer, MeterRegistry meterRegistry) {
        return new BackgroundJobServerMetricsBinder(backgroundJobServer, meterRegistry);
    }
}
