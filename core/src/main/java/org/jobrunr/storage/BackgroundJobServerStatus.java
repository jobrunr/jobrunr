package org.jobrunr.storage;

import org.jobrunr.server.jmx.BackgroundJobServerStatusMBean;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class BackgroundJobServerStatus implements BackgroundJobServerStatusMBean {

    private final UUID id;
    private final int workerPoolSize;
    private final int pollIntervalInSeconds;
    private final Duration deleteSucceededJobsAfter;
    private final Duration permanentlyDeleteDeletedJobsAfter;
    private volatile Instant firstHeartbeat;
    private volatile Instant lastHeartbeat;
    private volatile Boolean running;
    private final Long systemTotalMemory;
    private final Long systemFreeMemory;
    private final Double systemCpuLoad;
    private final Long processMaxMemory;
    private final Long processFreeMemory;
    private final Long processAllocatedMemory;
    private final Double processCpuLoad;

    public BackgroundJobServerStatus(int workerPoolSize, int pollIntervalInSeconds, Duration deleteSucceededJobsAfter, Duration permanentlyDeleteDeletedJobsAfter) {
        this(UUID.randomUUID(), workerPoolSize, pollIntervalInSeconds, deleteSucceededJobsAfter, permanentlyDeleteDeletedJobsAfter, null, null, true, null, null, null, null, null, null, null);
    }

    public BackgroundJobServerStatus(UUID id, int workerPoolSize, int pollIntervalInSeconds, Duration deleteSucceededJobsAfter, Duration permanentlyDeleteDeletedJobsAfter, Instant firstHeartbeat, Instant lastHeartbeat, boolean isRunning, Long systemTotalMemory, Long systemFreeMemory, Double systemCpuLoad, Long processMaxMemory, Long processFreeMemory, Long processAllocatedMemory, Double processCpuLoad) {
        this.id = id;
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
    public Instant getFirstHeartbeat() {
        return firstHeartbeat;
    }

    @Override
    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void start() {
        firstHeartbeat = Instant.now();
        running = true;
    }

    @Override
    public void pause() {
        running = false;
    }

    @Override
    public void resume() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
        firstHeartbeat = null;
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
