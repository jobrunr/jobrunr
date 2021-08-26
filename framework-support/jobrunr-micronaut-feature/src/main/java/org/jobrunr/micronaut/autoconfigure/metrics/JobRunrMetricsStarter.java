package org.jobrunr.micronaut.autoconfigure.metrics;

import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.server.metrics.BackgroundJobServerMetricsBinder;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Singleton
public class JobRunrMetricsStarter {

    private Logger LOGGER = LoggerFactory.getLogger(JobRunrMetricsStarter.class);

    @Inject
    private Optional<StorageProviderMetricsBinder> storageProviderMetricsBinder;

    @Inject
    private Optional<BackgroundJobServerMetricsBinder> backgroundJobServerMetrics;

    @EventListener
    void startup(ServerStartupEvent event) {
        storageProviderMetricsBinder.ifPresent(x -> LOGGER.debug("JobRunr StorageProvider MicroMeter Metrics enabled"));
        backgroundJobServerMetrics.ifPresent(x -> LOGGER.debug("JobRunr BackgroundJobServer MicroMeter Metrics enabled"));
    }
}
