package org.jobrunr.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.listeners.JobStatsChangeListener;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

public class StorageProviderMetricsBinder implements JobStatsChangeListener {

    private final StorageProvider storageProvider;
    private final MeterRegistry meterRegistry;
    private final AtomicLong scheduledGauge = new AtomicLong(0);
    private final AtomicLong enqueuedGauge = new AtomicLong(0);
    private final AtomicLong processingGauge = new AtomicLong(0);
    private final AtomicLong failedGauge = new AtomicLong(0);
    private final AtomicLong succeededGauge = new AtomicLong(0);
    private final AtomicLong allTimeSucceededGauge = new AtomicLong(0);
    private final AtomicLong deletedGauge = new AtomicLong(0);


    public StorageProviderMetricsBinder(StorageProvider storageProvider, MeterRegistry meterRegistry) {
        this.storageProvider = storageProvider;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void setUpMetrics() {
        this.registryStorageProvider();
    }

    private void registryStorageProvider() {
        registerGauge(StateName.SCHEDULED, scheduledGauge);
        registerGauge(StateName.ENQUEUED, enqueuedGauge);
        registerGauge(StateName.PROCESSING, processingGauge);
        registerGauge(StateName.FAILED, failedGauge);
        registerGauge(StateName.SUCCEEDED, succeededGauge);
        registerGauge("all-time-succeeded", allTimeSucceededGauge);
        registerGauge(StateName.DELETED, deletedGauge);

        this.storageProvider.addJobStorageOnChangeListener(this);
    }

    private void registerGauge(StateName stateName, AtomicLong number) {
        registerGauge(stateName.toString(), number);
    }

    private void registerGauge(String stateName, AtomicLong number) {
        meterRegistry.gauge("jobrunr.jobs", Tags.of("state", stateName), number);
    }

    @Override
    public void onChange(JobStats jobStats) {
        scheduledGauge.set(jobStats.getScheduled());
        enqueuedGauge.set(jobStats.getEnqueued());
        processingGauge.set(jobStats.getProcessing());
        failedGauge.set(jobStats.getFailed());
        succeededGauge.set(jobStats.getSucceeded());
        allTimeSucceededGauge.set(jobStats.getAllTimeSucceeded());
        deletedGauge.set(jobStats.getDeleted());
    }
}
