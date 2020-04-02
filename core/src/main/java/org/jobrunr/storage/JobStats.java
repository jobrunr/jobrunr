package org.jobrunr.storage;

import java.time.Instant;

public class JobStats {

    private Instant timeStamp;
    private Long total;
    private Long awaiting;
    private Long scheduled;
    private Long enqueued;
    private Long processing;
    private Long failed;
    private Long succeeded;
    private int backgroundJobServers;

    public static JobStats empty() {
        return new JobStats(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0);
    }

    public JobStats(Long total, Long awaiting, Long scheduled, Long enqueued, Long processing, Long failed, Long succeeded, int backgroundJobServers) {
        this.timeStamp = Instant.now();
        this.total = total;
        this.awaiting = awaiting;
        this.scheduled = scheduled;
        this.enqueued = enqueued;
        this.processing = processing;
        this.failed = failed;
        this.succeeded = succeeded;
        this.backgroundJobServers = backgroundJobServers;
    }

    public Instant getTimeStamp() {
        return timeStamp;
    }

    public Long getTotal() {
        return total;
    }

    public Long getAwaiting() {
        return awaiting;
    }

    public Long getScheduled() {
        return scheduled;
    }

    public Long getEnqueued() {
        return enqueued;
    }

    public Long getProcessing() {
        return processing;
    }

    public Long getFailed() {
        return failed;
    }

    public Long getSucceeded() {
        return succeeded;
    }

    public int getBackgroundJobServers() {
        return backgroundJobServers;
    }
}
