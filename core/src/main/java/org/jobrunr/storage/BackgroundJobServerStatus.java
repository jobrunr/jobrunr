package org.jobrunr.storage;

import java.time.Instant;
import java.util.UUID;

public class BackgroundJobServerStatus {

    private final UUID id;
    private final int workerPoolSize;
    private final int pollIntervalInSeconds;
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

    public BackgroundJobServerStatus(int pollIntervalInSeconds, int workerPoolSize) {
        this(UUID.randomUUID(), workerPoolSize, pollIntervalInSeconds, null, null, true, null, null, null, null, null, null, null);
    }

    public BackgroundJobServerStatus(UUID id, int workerPoolSize, int pollIntervalInSeconds, Instant firstHeartbeat, Instant lastHeartbeat, boolean isRunning, Long systemTotalMemory, Long systemFreeMemory, Double systemCpuLoad, Long processMaxMemory, Long processFreeMemory, Long processAllocatedMemory, Double processCpuLoad) {
        this.id = id;
        this.workerPoolSize = workerPoolSize;
        this.pollIntervalInSeconds = pollIntervalInSeconds;
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

    public UUID getId() {
        return id;
    }

    public int getWorkerPoolSize() {
        return workerPoolSize;
    }

    public Instant getFirstHeartbeat() {
        return firstHeartbeat;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public int getPollIntervalInSeconds() {
        return pollIntervalInSeconds;
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        firstHeartbeat = Instant.now();
        running = true;
    }

    public void pause() {
        running = false;
    }

    public void resume() {
        running = true;
    }

    public void stop() {
        running = false;
        firstHeartbeat = null;
    }

    public Long getSystemTotalMemory() {
        return systemTotalMemory;
    }

    public Long getSystemFreeMemory() {
        return systemFreeMemory;
    }

    public Double getSystemCpuLoad() {
        return systemCpuLoad;
    }

    public Long getProcessMaxMemory() {
        return processMaxMemory;
    }

    public Long getProcessFreeMemory() {
        return processFreeMemory;
    }

    public Long getProcessAllocatedMemory() {
        return processAllocatedMemory;
    }

    public Double getProcessCpuLoad() {
        return processCpuLoad;
    }
}
