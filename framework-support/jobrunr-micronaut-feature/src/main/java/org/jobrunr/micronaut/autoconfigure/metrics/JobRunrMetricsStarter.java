package org.jobrunr.micronaut.autoconfigure.metrics;

import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import org.jobrunr.server.metrics.BackgroundJobServerMetricsBinder;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Singleton
public class JobRunrMetricsStarter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunrMetricsStarter.class);

    private final Optional<StorageProviderMetricsBinder> storageProviderMetricsBinder;

    private final Optional<BackgroundJobServerMetricsBinder> backgroundJobServerMetrics;

    public JobRunrMetricsStarter(Optional<StorageProviderMetricsBinder> storageProviderMetricsBinder, Optional<BackgroundJobServerMetricsBinder> backgroundJobServerMetrics) {
        this.storageProviderMetricsBinder = storageProviderMetricsBinder;
        this.backgroundJobServerMetrics = backgroundJobServerMetrics;
    }

    @EventListener
    void startup(ServerStartupEvent event) {
        storageProviderMetricsBinder.ifPresent(x -> LOGGER.debug("JobRunr StorageProvider MicroMeter Metrics enabled"));
        backgroundJobServerMetrics.ifPresent(x -> LOGGER.debug("JobRunr BackgroundJobServer MicroMeter Metrics enabled"));
    }
}
