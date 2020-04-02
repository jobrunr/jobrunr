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

    public BackgroundJobServerStatus(int pollIntervalInSeconds, int workerPoolSize) {
        this(UUID.randomUUID(), workerPoolSize, pollIntervalInSeconds, null, null, true);
    }

    public BackgroundJobServerStatus(UUID id, int workerPoolSize, int pollIntervalInSeconds, Instant firstHeartbeat, Instant lastHeartbeat, boolean isRunning) {
        this.id = id;
        this.workerPoolSize = workerPoolSize;
        this.pollIntervalInSeconds = pollIntervalInSeconds;
        this.firstHeartbeat = firstHeartbeat;
        this.lastHeartbeat = lastHeartbeat;
        this.running = isRunning;
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
}
