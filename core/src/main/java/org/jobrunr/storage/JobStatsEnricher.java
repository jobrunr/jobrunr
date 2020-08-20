package org.jobrunr.storage;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

import static java.time.temporal.ChronoUnit.SECONDS;

public class JobStatsEnricher {

    private final ReentrantLock lock = new ReentrantLock();
    private JobStats firstRelevantJobStats;
    private JobStats previousJobStats;
    private JobStatsExtended jobStatsExtended;

    public JobStatsExtended enrich(JobStats jobStats) {
        JobStats latestJobStats = getLatestJobStats(jobStats, previousJobStats);
        if (lock.tryLock()) {
            setFirstRelevantJobStats(latestJobStats);
            setJobStatsExtended(latestJobStats);
            setPreviousJobStats(latestJobStats);
            lock.unlock();
        }
        if (jobStatsExtended == null) return new JobStatsExtended(latestJobStats);
        return jobStatsExtended;
    }

    private static JobStats getLatestJobStats(JobStats jobStats, JobStats previousJobStats) {
        if (previousJobStats == null) return jobStats;
        if (jobStats.getTimeStamp().isAfter(previousJobStats.getTimeStamp())) return jobStats;
        return previousJobStats;
    }

    private void setFirstRelevantJobStats(JobStats jobStats) {
        if (firstRelevantJobStats == null) {
            firstRelevantJobStats = jobStats;
        } else if (jobStats.getEnqueued() < 1 && jobStats.getProcessing() < 1) {
            firstRelevantJobStats = jobStats;
        } else if (jobStats.getEnqueued() > firstRelevantJobStats.getEnqueued()) {
            firstRelevantJobStats = jobStats;
        }
    }

    private void setJobStatsExtended(JobStats jobStats) {
        JobStats previousJobStats = this.previousJobStats != null ? this.previousJobStats : this.firstRelevantJobStats;
        Long amountSucceeded = jobStats.getSucceeded() - previousJobStats.getSucceeded();
        Long amountFailed = jobStats.getFailed() - previousJobStats.getFailed();
        Instant estimatedProcessingFinishedInstant = estimatedProcessingFinishedInstant(firstRelevantJobStats, jobStats);
        jobStatsExtended = new JobStatsExtended(jobStats, amountSucceeded, amountFailed, estimatedProcessingFinishedInstant);
    }

    private Instant estimatedProcessingFinishedInstant(JobStats firstRelevantJobStats, JobStats jobStats) {
        if (jobStats.getSucceeded() - firstRelevantJobStats.getSucceeded() < 1) return null;
        Duration durationForAmountSucceeded = Duration.between(firstRelevantJobStats.getTimeStamp(), jobStats.getTimeStamp());
        Long amountSucceededPerSecond = Double.valueOf(Math.ceil((jobStats.getSucceeded() - firstRelevantJobStats.getSucceeded()) / durationForAmountSucceeded.get(SECONDS))).longValue();
        long processingTimeInSeconds = Double.valueOf(Math.ceil((jobStats.getEnqueued() + jobStats.getProcessing()) / amountSucceededPerSecond)).longValue();
        return Instant.now().plusSeconds(processingTimeInSeconds);
    }

    private void setPreviousJobStats(JobStats jobStats) {
        previousJobStats = jobStats;
    }

}
