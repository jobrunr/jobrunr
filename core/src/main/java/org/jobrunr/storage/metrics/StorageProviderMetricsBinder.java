package org.jobrunr.storage.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.listeners.JobStatsChangeListener;

import java.util.concurrent.atomic.AtomicLong;

public class StorageProviderMetricsBinder implements JobStatsChangeListener, AutoCloseable {

    private final StorageProvider storageProvider;
    private final MeterRegistry meterRegistry;
    private final AtomicLong recurringJobsGauge = new AtomicLong(0);
    private final AtomicLong scheduledGauge = new AtomicLong(0);
    private final AtomicLong enqueuedGauge = new AtomicLong(0);
    private final AtomicLong processingGauge = new AtomicLong(0);
    private final AtomicLong failedGauge = new AtomicLong(0);
    private final AtomicLong succeededGauge = new AtomicLong(0);
    private final AtomicLong allTimeSucceededGauge = new AtomicLong(0);
    private final AtomicLong backgroundJobServersGauge = new AtomicLong(0);
    private final AtomicLong deletedGauge = new AtomicLong(0);


    public StorageProviderMetricsBinder(StorageProvider storageProvider, MeterRegistry meterRegistry) {
        this.storageProvider = storageProvider;
        this.meterRegistry = meterRegistry;
        registerStorageProviderMetrics();
    }

    public void registerStorageProviderMetrics() {
        registerGauge("RECURRING", recurringJobsGauge);
        registerGauge(StateName.SCHEDULED, scheduledGauge);
        registerGauge(StateName.ENQUEUED, enqueuedGauge);
        registerGauge(StateName.PROCESSING, processingGauge);
        registerGauge(StateName.FAILED, failedGauge);
        registerGauge(StateName.SUCCEEDED, succeededGauge);
        registerGauge("ALL_TIME_SUCCEEDED", allTimeSucceededGauge);
        registerGauge(StateName.DELETED, deletedGauge);
        registerGauge("BACKGROUND_JOB_SERVERS", backgroundJobServersGauge);

        onChange(this.storageProvider.getJobStats());
        this.storageProvider.addJobStorageOnChangeListener(this);
    }

    private void registerGauge(StateName stateName, AtomicLong number) {
        registerGauge(stateName.toString(), number);
    }

    private void registerGauge(String stateName, AtomicLong number) {
        meterRegistry.gauge("jobrunr.jobs.by-state", Tags.of("state", stateName), number);
    }

    @Override
    public void onChange(JobStats jobStats) {
        recurringJobsGauge.set(jobStats.getRecurringJobs());
        scheduledGauge.set(jobStats.getScheduled());
        enqueuedGauge.set(jobStats.getEnqueued());
        processingGauge.set(jobStats.getProcessing());
        failedGauge.set(jobStats.getFailed());
        succeededGauge.set(jobStats.getSucceeded());
        allTimeSucceededGauge.set(jobStats.getAllTimeSucceeded());
        deletedGauge.set(jobStats.getDeleted());
        backgroundJobServersGauge.set(jobStats.getBackgroundJobServers());
    }

    @Override
    public void close() {
        this.storageProvider.removeJobStorageOnChangeListener(this);
    }
}
