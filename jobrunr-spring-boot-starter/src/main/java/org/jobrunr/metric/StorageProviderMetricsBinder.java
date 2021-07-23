package org.jobrunr.metric;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.StorageProvider;

import javax.annotation.PostConstruct;
import java.util.Arrays;

public class StorageProviderMetricsBinder {

    private final MeterRegistry registry;
    private final StorageProvider storageProvider;

    public StorageProviderMetricsBinder(MeterRegistry registry, StorageProvider storageProvider) {
        this.registry = registry;
        this.storageProvider = storageProvider;
    }

    @PostConstruct
    public void setUpMetrics() {
        this.registryStorageProvider();
    }

    private void registryStorageProvider() {
        String PREFIX = "jobrunr_";
        String jobs = PREFIX + "jobs";
        Arrays.stream(StateName.values())
                .forEach((state) -> Gauge
                        .builder(jobs, this.storageProvider, (provider) -> (double) provider.getJobPage(state, PageRequest.ascOnUpdatedAt(0)).getTotal())
                        .tag("state", state.toString()).register(this.registry));
    }
}
