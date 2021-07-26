package org.jobrunr.metric;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.listeners.JobStatsChangeListener;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class StorageProviderMetricsBinder implements JobStatsChangeListener {

    private final StorageProvider storageProvider;
    private final Map<StateName, AtomicInteger> atomics = new HashMap<>();

    public StorageProviderMetricsBinder(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    @PostConstruct
    public void setUpMetrics() {
        this.registryStorageProvider();
    }

    private void registryStorageProvider() {
        String PREFIX = "jobrunr_";
        String jobs = PREFIX + "jobs";

        Arrays.stream(StateName.values()).forEach((state) ->
                atomics.put(state, Metrics.gauge(jobs, Tags.of("state", state.toString()), new AtomicInteger(0)))
        );

        this.storageProvider.addJobStorageOnChangeListener(this);
    }

    @Override
    public void onChange(JobStats jobStats) {
        atomics.get(StateName.AWAITING).set(jobStats.getAwaiting().intValue());
        atomics.get(StateName.SCHEDULED).set(jobStats.getScheduled().intValue());
        atomics.get(StateName.ENQUEUED).set(jobStats.getEnqueued().intValue());
        atomics.get(StateName.PROCESSING).set(jobStats.getProcessing().intValue());
        atomics.get(StateName.FAILED).set(jobStats.getFailed().intValue());
        atomics.get(StateName.SUCCEEDED).set(jobStats.getSucceeded().intValue());
        atomics.get(StateName.DELETED).set(jobStats.getDeleted().intValue());
    }
}
