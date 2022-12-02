package org.jobrunr.storage;

import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.jmx.JobServerStats;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class BackgroundJobServerStatusTestBuilder {

    public static final String DEFAULT_SERVER_NAME = "test-server-name";

    private final JobServerStats jobServerStats = new JobServerStats();
    private UUID id = UUID.randomUUID();
    private String name = DEFAULT_SERVER_NAME;
    private int workerPoolSize = 10;
    private int pollIntervalInSeconds = BackgroundJobServerConfiguration.DEFAULT_POLL_INTERVAL_IN_SECONDS;
    private Duration deleteSucceededJobsAfter = BackgroundJobServerConfiguration.DEFAULT_DELETE_SUCCEEDED_JOBS_DURATION;
    private Duration permanentlyDeleteDeletedJobsAfter = BackgroundJobServerConfiguration.DEFAULT_PERMANENTLY_DELETE_JOBS_DURATION;
    private Instant firstHeartbeat;
    private Instant lastHeartbeat;
    private boolean running;

    private BackgroundJobServerStatusTestBuilder() {

    }

    public static BackgroundJobServerStatusTestBuilder aFastBackgroundJobServerStatus() {
        return new BackgroundJobServerStatusTestBuilder()
                .withPollIntervalInSeconds(5);
    }

    public static BackgroundJobServerStatusTestBuilder aDefaultBackgroundJobServerStatus() {
        return new BackgroundJobServerStatusTestBuilder();
    }

    public static BackgroundJobServerStatusTestBuilder aBackgroundJobServerStatusBasedOn(BackgroundJobServerStatus status) {
        return new BackgroundJobServerStatusTestBuilder()
                .withId(status.getId())
                .withName(status.getName())
                .withWorkerSize(status.getWorkerPoolSize())
                .withPollIntervalInSeconds(status.getPollIntervalInSeconds())
                .withRunning(status.isRunning())
                .withFirstHeartbeat(status.getFirstHeartbeat())
                .withLastHeartbeat(status.getLastHeartbeat());
    }

    public BackgroundJobServerStatusTestBuilder withId() {
        this.id = UUID.randomUUID();
        return this;
    }

    public BackgroundJobServerStatusTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public BackgroundJobServerStatusTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public BackgroundJobServerStatusTestBuilder withPollIntervalInSeconds(int pollIntervalInSeconds) {
        this.pollIntervalInSeconds = pollIntervalInSeconds;
        return this;
    }

    public BackgroundJobServerStatusTestBuilder withWorkerSize(int workerPoolSize) {
        this.workerPoolSize = workerPoolSize;
        return this;
    }

    public BackgroundJobServerStatusTestBuilder withFirstHeartbeat(Instant firstHeartbeat) {
        this.firstHeartbeat = firstHeartbeat;
        return this;
    }

    public BackgroundJobServerStatusTestBuilder withLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
        return this;
    }

    public BackgroundJobServerStatusTestBuilder withRunning(boolean running) {
        this.running = running;
        return this;
    }

    public BackgroundJobServerStatusTestBuilder withIsStarted() {
        this.firstHeartbeat = Instant.now().minusMillis(1);
        this.lastHeartbeat = Instant.now();
        this.running = true;
        return this;
    }

    public BackgroundJobServerStatus build() {
        return new BackgroundJobServerStatus(id, name, workerPoolSize, pollIntervalInSeconds,
                deleteSucceededJobsAfter, permanentlyDeleteDeletedJobsAfter, firstHeartbeat, lastHeartbeat, running,
                jobServerStats.getSystemTotalMemory(), jobServerStats.getSystemFreeMemory(), jobServerStats.getSystemCpuLoad(), jobServerStats.getProcessMaxMemory(),
                jobServerStats.getProcessFreeMemory(), jobServerStats.getProcessAllocatedMemory(), jobServerStats.getProcessCpuLoad());
    }
}