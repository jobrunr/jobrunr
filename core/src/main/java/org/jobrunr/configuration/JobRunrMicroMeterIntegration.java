package org.jobrunr.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.metrics.BackgroundJobServerMetricsBinder;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;

/**
 * A wrapper around a Micrometer {@link MeterRegistry} that allows to integrate Micrometer with JobRunr.
 *
 * This wrapper is needed as otherwise the JobRunrConfiguration class would have a dependency on Micrometer which is optional.
 */
public class JobRunrMicroMeterIntegration implements AutoCloseable {

    private final MeterRegistry meterRegistry;
    private StorageProviderMetricsBinder storageProviderMetricsBinder;
    private BackgroundJobServerMetricsBinder backgroundJobServerMetricsBinder;

    public JobRunrMicroMeterIntegration(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void initialize(StorageProvider storageProvider, BackgroundJobServer backgroundJobServer) {
        storageProviderMetricsBinder = new StorageProviderMetricsBinder(storageProvider, meterRegistry);
        if(backgroundJobServer != null) {
            backgroundJobServerMetricsBinder = new BackgroundJobServerMetricsBinder(backgroundJobServer, meterRegistry);
        }
    }

    public void close() {
        storageProviderMetricsBinder.close();
        if(backgroundJobServerMetricsBinder != null) {
            backgroundJobServerMetricsBinder.close();
        }
    }
}
