package org.jobrunr.quarkus.autoconfigure.metrics;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import org.jboss.logging.Logger;
import org.jobrunr.server.metrics.BackgroundJobServerMetricsBinder;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;

@Dependent
public class JobRunrMetricsStarter {

    private final Logger LOGGER = Logger.getLogger(JobRunrMetricsStarter.class);

    Instance<StorageProviderMetricsBinder> storageProviderMetricsBinderInstance;

    Instance<BackgroundJobServerMetricsBinder> backgroundJobServerMetricsBinderInstance;

    public JobRunrMetricsStarter(Instance<StorageProviderMetricsBinder> storageProviderMetricsBinderInstance, Instance<BackgroundJobServerMetricsBinder> backgroundJobServerMetricsBinderInstance) {
        this.storageProviderMetricsBinderInstance = storageProviderMetricsBinderInstance;
        this.backgroundJobServerMetricsBinderInstance = backgroundJobServerMetricsBinderInstance;
    }

    void startup(@Observes StartupEvent event) {
        if (storageProviderMetricsBinderInstance.isResolvable()) {
            storageProviderMetricsBinderInstance.get();
            LOGGER.debug("JobRunr StorageProvider MicroMeter Metrics enabled");
        }
        if (backgroundJobServerMetricsBinderInstance.isResolvable()) {
            backgroundJobServerMetricsBinderInstance.get();
            LOGGER.debug("JobRunr BackgroundJobServer MicroMeter Metrics enabled");
        }
    }
}
