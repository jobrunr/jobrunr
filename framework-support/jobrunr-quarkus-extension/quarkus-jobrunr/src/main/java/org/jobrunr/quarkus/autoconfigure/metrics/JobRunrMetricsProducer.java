package org.jobrunr.quarkus.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;
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
        JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;

        @Produces
        @DefaultBean
        @Singleton
        @LookupIfProperty(name = "quarkus.jobrunr.jobs.metrics.enabled", stringValue = "true")
        public StorageProviderMetricsBinder storageProviderMetricsBinder(StorageProvider storageProvider, MeterRegistry meterRegistry) {
            if (jobRunrRuntimeConfiguration.jobs().metrics().enabled()) {
                return new StorageProviderMetricsBinder(storageProvider, meterRegistry);
            }
            return null;
        }
    }

    public static class BackgroundJobServerMetricsProducer {
        @Inject
        JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;

        @Produces
        @DefaultBean
        @Singleton
        @LookupIfProperty(name = "quarkus.jobrunr.background-job-server.enabled", stringValue = "true")
        @LookupIfProperty(name = "quarkus.jobrunr.background-job-server.metrics.enabled", stringValue = "true")
        public BackgroundJobServerMetricsBinder backgroundJobServerMetricsBinder(Instance<BackgroundJobServer> backgroundJobServer, MeterRegistry meterRegistry) {
            if (jobRunrRuntimeConfiguration.backgroundJobServer().enabled() && jobRunrRuntimeConfiguration.backgroundJobServer().metrics().enabled()) {
                return new BackgroundJobServerMetricsBinder(backgroundJobServer.get(), meterRegistry);
            }
            return null;
        }
    }
}
