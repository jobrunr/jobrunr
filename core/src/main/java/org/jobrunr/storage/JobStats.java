package org.jobrunr.storage;

import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;

public class JobStats implements Comparable<JobStats> {

    private final Instant timeStamp;
    private final Long queryDurationInMillis;
    private final Long total;
    private final Long awaiting;
    private final Long scheduled;
    private final Long enqueued;
    private final Long processing;
    private final Long failed;
    private final Long succeeded;
    private final Long allTimeSucceeded;
    private final Long deleted;
    private final int recurringJobs;
    private final int backgroundJobServers;

    public static JobStats empty() {
        return new JobStats(now(), 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0, 0);
    }

    public static JobStats of(Instant instant, JobStats jobStats) {
        return new JobStats(instant, jobStats.getTotal(), jobStats.getAwaiting(), jobStats.getScheduled(), jobStats.getEnqueued(), jobStats.getProcessing(), jobStats.getFailed(), jobStats.getSucceeded(), jobStats.getAllTimeSucceeded(), jobStats.getDeleted(), jobStats.getRecurringJobs(), jobStats.getBackgroundJobServers());
    }

    protected JobStats(JobStats jobStats) {
        this(jobStats.getTimeStamp(), jobStats.getTotal(), jobStats.getAwaiting(), jobStats.getScheduled(), jobStats.getEnqueued(), jobStats.getProcessing(), jobStats.getFailed(), jobStats.getSucceeded(), jobStats.getAllTimeSucceeded(), jobStats.getDeleted(), jobStats.getRecurringJobs(), jobStats.getBackgroundJobServers());
    }

    public JobStats(Instant timeStamp, Long total, Long awaiting, Long scheduled, Long enqueued, Long processing, Long failed, Long succeeded, Long allTimeSucceeded, Long deleted, int recurringJobs, int backgroundJobServers) {
        this.timeStamp = timeStamp;
        this.queryDurationInMillis = Duration.between(timeStamp, now()).toMillis();
        this.total = total;
        this.awaiting = awaiting;
        this.scheduled = scheduled;
        this.enqueued = enqueued;
        this.processing = processing;
        this.failed = failed;
        this.succeeded = succeeded;
        this.allTimeSucceeded = allTimeSucceeded;
        this.deleted = deleted;
        this.recurringJobs = recurringJobs;
        this.backgroundJobServers = backgroundJobServers;
    }

    public Instant getTimeStamp() {
        return timeStamp;
    }

    public Long getQueryDurationInMillis() {
        return queryDurationInMillis;
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

    public Long getAllTimeSucceeded() {
        return allTimeSucceeded;
    }

    public Long getDeleted() {
        return deleted;
    }

    public int getRecurringJobs() {
        return recurringJobs;
    }

    public int getBackgroundJobServers() {
        return backgroundJobServers;
    }

    @Override
    public int compareTo(JobStats jobStats) {
        if (this.succeeded > jobStats.succeeded) return 1;
        else if (this.allTimeSucceeded > jobStats.allTimeSucceeded) return 1;
        else return this.timeStamp.compareTo(jobStats.timeStamp);
    }
}
