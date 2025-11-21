package org.jobrunr.storage;

import org.jobrunr.server.jmx.BackgroundJobServerStatusMBean;
import org.jobrunr.server.jmx.JobServerStats;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class BackgroundJobServerStatus implements BackgroundJobServerStatusMBean {

    private final UUID id;
    private final String name;
    private final int workerPoolSize;
    private final int pollIntervalInSeconds;
    private final Duration deleteSucceededJobsAfter;
    private final Duration permanentlyDeleteDeletedJobsAfter;
    private final @Nullable Instant firstHeartbeat;
    private final @Nullable Instant lastHeartbeat;
    private final Boolean running;
    private final Long systemTotalMemory;
    private final Long systemFreeMemory;
    private final Double systemCpuLoad;
    private final Long processMaxMemory;
    private final Long processFreeMemory;
    private final Long processAllocatedMemory;
    private final Double processCpuLoad;

    public BackgroundJobServerStatus(UUID id, String name, int workerCount, int pollIntervalInSeconds, Duration deleteSucceededJobsAfter, Duration permanentlyDeleteDeletedJobsAfter, @Nullable Instant firstHeartbeat, @Nullable Instant lastHeartbeat, boolean isRunning, JobServerStats jobServerStats) {
        this(id, name, workerCount, pollIntervalInSeconds, deleteSucceededJobsAfter, permanentlyDeleteDeletedJobsAfter, firstHeartbeat, lastHeartbeat, isRunning,
                jobServerStats.getSystemTotalMemory(), jobServerStats.getSystemFreeMemory(), jobServerStats.getSystemCpuLoad(),
                jobServerStats.getProcessMaxMemory(), jobServerStats.getProcessFreeMemory(),
                jobServerStats.getProcessAllocatedMemory(), jobServerStats.getProcessCpuLoad());
    }

    public BackgroundJobServerStatus(UUID id, String name, int workerPoolSize, int pollIntervalInSeconds, Duration deleteSucceededJobsAfter, Duration permanentlyDeleteDeletedJobsAfter, @Nullable Instant firstHeartbeat, @Nullable Instant lastHeartbeat, boolean isRunning, Long systemTotalMemory, Long systemFreeMemory, Double systemCpuLoad, Long processMaxMemory, Long processFreeMemory, Long processAllocatedMemory, Double processCpuLoad) {
        this.id = id;
        this.name = name;
        this.workerPoolSize = workerPoolSize;
        this.pollIntervalInSeconds = pollIntervalInSeconds;
        this.deleteSucceededJobsAfter = deleteSucceededJobsAfter;
        this.permanentlyDeleteDeletedJobsAfter = permanentlyDeleteDeletedJobsAfter;
        this.firstHeartbeat = firstHeartbeat;
        this.lastHeartbeat = lastHeartbeat;
        this.running = isRunning;
        this.systemTotalMemory = systemTotalMemory;
        this.systemFreeMemory = systemFreeMemory;
        this.systemCpuLoad = systemCpuLoad;
        this.processMaxMemory = processMaxMemory;
        this.processFreeMemory = processFreeMemory;
        this.processAllocatedMemory = processAllocatedMemory;
        this.processCpuLoad = processCpuLoad;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getWorkerPoolSize() {
        return workerPoolSize;
    }

    @Override
    public int getPollIntervalInSeconds() {
        return pollIntervalInSeconds;
    }

    @Override
    public Duration getDeleteSucceededJobsAfter() {
        return deleteSucceededJobsAfter;
    }

    @Override
    public Duration getPermanentlyDeleteDeletedJobsAfter() {
        return permanentlyDeleteDeletedJobsAfter;
    }

    @Override
    public @Nullable Instant getFirstHeartbeat() {
        return firstHeartbeat;
    }

    @Override
    public @Nullable Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public Long getSystemTotalMemory() {
        return systemTotalMemory;
    }

    @Override
    public Long getSystemFreeMemory() {
        return systemFreeMemory;
    }

    @Override
    public Double getSystemCpuLoad() {
        return systemCpuLoad;
    }

    @Override
    public Long getProcessMaxMemory() {
        return processMaxMemory;
    }

    @Override
    public Long getProcessFreeMemory() {
        return processFreeMemory;
    }

    @Override
    public Long getProcessAllocatedMemory() {
        return processAllocatedMemory;
    }

    @Override
    public Double getProcessCpuLoad() {
        return processCpuLoad;
    }
}
