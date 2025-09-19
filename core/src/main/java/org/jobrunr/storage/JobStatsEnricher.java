package org.jobrunr.storage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.ceil;
import static org.jobrunr.utils.NumberUtils.isZero;

/**
 * Class which takes JobStats and extends them with estimations on how long the work will take based on previous JobStats.
 */
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
        return jobStatsExtended;
    }

    private static JobStats getLatestJobStats(JobStats jobStats, JobStats previousJobStats) {
        if (previousJobStats == null) return jobStats;
        else if (jobStats.compareTo(previousJobStats) >= 0) return jobStats;
        else return previousJobStats;
    }

    private void setFirstRelevantJobStats(JobStats jobStats) {
        if (firstRelevantJobStats == null
                || (jobStats.getEnqueued() < 1 && jobStats.getProcessing() < 1)
                || (jobStats.getEnqueued() > firstRelevantJobStats.getEnqueued())) {
            firstRelevantJobStats = jobStats;
        }
    }

    private void setJobStatsExtended(JobStats jobStats) {
        JobStats actualPreviousJobStats = this.previousJobStats != null ? this.previousJobStats : this.firstRelevantJobStats;
        Long amountSucceeded = jobStats.getSucceeded() - actualPreviousJobStats.getSucceeded();
        Long amountFailed = jobStats.getFailed() - actualPreviousJobStats.getFailed();
        Instant estimatedProcessingFinishedInstant = estimatedProcessingFinishedInstant(firstRelevantJobStats, jobStats);
        if (estimatedProcessingFinishedInstant != null) {
            jobStatsExtended = new JobStatsExtended(jobStats, amountSucceeded, amountFailed, estimatedProcessingFinishedInstant);
        } else if (jobStatsExtended != null && jobStatsExtended.getEstimation().isEstimatedProcessingFinishedInstantAvailable()) {
            jobStatsExtended = new JobStatsExtended(jobStats, amountSucceeded, amountFailed, jobStatsExtended.getEstimation().getEstimatedProcessingFinishedAt());
        } else {
            jobStatsExtended = new JobStatsExtended(jobStats);
        }
    }

    private Instant estimatedProcessingFinishedInstant(JobStats firstRelevantJobStats, JobStats jobStats) {
        if (jobStats.getSucceeded() - firstRelevantJobStats.getSucceeded() < 1) return null;
        BigDecimal durationForAmountSucceededInSeconds = BigDecimal.valueOf(Duration.between(firstRelevantJobStats.getTimeStamp(), jobStats.getTimeStamp()).getSeconds());
        if (isZero(durationForAmountSucceededInSeconds)) return null;
        BigDecimal amountSucceededPerSecond = BigDecimal.valueOf(ceil((double) jobStats.getSucceeded() - firstRelevantJobStats.getSucceeded())).divide(durationForAmountSucceededInSeconds, RoundingMode.CEILING);
        if (isZero(amountSucceededPerSecond)) return null;
        BigDecimal processingTimeInSeconds = BigDecimal.valueOf(jobStats.getEnqueued() + jobStats.getProcessing()).divide(amountSucceededPerSecond, RoundingMode.HALF_UP);
        return Instant.now().plusSeconds(processingTimeInSeconds.longValue());
    }

    private void setPreviousJobStats(JobStats jobStats) {
        previousJobStats = jobStats;
    }

}
